package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.AbstractBaseMockServer
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder
import au.com.dius.pact.consumer.junit.JUnitTestSupport
import au.com.dius.pact.consumer.junit.MockServerConfig
import au.com.dius.pact.consumer.mockServer
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactDirectory
import au.com.dius.pact.core.model.annotations.PactFolder
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.Annotations
import au.com.dius.pact.core.support.BuiltToolConfig
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.MetricEvent
import au.com.dius.pact.core.support.Metrics
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser
import au.com.dius.pact.core.support.isNotEmpty
import mu.KLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import org.junit.platform.commons.util.AnnotationUtils.isAnnotated
import java.lang.reflect.Method
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.findAnnotation

class PactConsumerTestExt : Extension, BeforeTestExecutionCallback, BeforeAllCallback, ParameterResolver,
  AfterTestExecutionCallback, AfterAllCallback {

  private val ep: ExpressionParser = ExpressionParser()

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val providers = lookupProviderInfo(extensionContext)
    val type = parameterContext.parameter.type

    if (type.isAssignableFrom(MockServer::class.java)) {
      return if (mockServerConfigured(extensionContext)) {
        true
      } else {
        providers.any {
          it.first.providerType == null ||
            it.first.providerType == ProviderType.SYNCH ||
            it.first.providerType == ProviderType.UNSPECIFIED
        }
      }
    } else {
      if (providers.any { it.first.providerType == ProviderType.ASYNCH }) {
        when {
          type.isAssignableFrom(List::class.java) -> return true
          type.isAssignableFrom(V4Pact::class.java) -> return true
          type.isAssignableFrom(MessagePact::class.java) -> return true
          type.isAssignableFrom(V4Interaction.AsynchronousMessage::class.java) -> return true
        }
      }

      if (providers.any { it.first.providerType == ProviderType.SYNCH_MESSAGE }) {
        when {
          type.isAssignableFrom(List::class.java) -> return true
          type.isAssignableFrom(V4Pact::class.java) -> return true
          type.isAssignableFrom(V4Interaction.SynchronousMessages::class.java) -> return true
        }
      }

      if (providers.any {
          it.first.providerType == null ||
            it.first.providerType == ProviderType.SYNCH ||
            it.first.providerType == ProviderType.UNSPECIFIED
        }) {
        when {
          type.isAssignableFrom(RequestResponsePact::class.java) -> return true
          type.isAssignableFrom(V4Pact::class.java) -> return true
          type.isAssignableFrom(V4Interaction.SynchronousHttp::class.java) -> return true
        }
      }
    }

    return false
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
    val type = parameterContext.parameter.type
    val providers = lookupProviderInfo(extensionContext)
    return when {
      providers.size == 1 -> resolveParameterForProvider(providers[0], extensionContext, type)
      parameterContext.isAnnotated(ForProvider::class.java) -> {
        val providerName = parameterContext.findAnnotation(ForProvider::class.java).get().value
        val providerInfo = providers.find { (provider, _) -> provider.providerName == providerName }
        if (providerInfo != null) {
          resolveParameterForProvider(providerInfo, extensionContext, type)
        } else {
          throw UnsupportedOperationException("Did not find a provider with name '${providerName}' for " +
            " parameter: ${parameterContext.index}, ${parameterContext.parameter}")
        }
      }
      else -> {
        throw UnsupportedOperationException("You have setup multiple providers for this test. You need to specify" +
          " which provider the injected value is for with the @ForProvider annotation." +
          " Parameter: ${parameterContext.index}, ${parameterContext.parameter}")
      }
    }
  }

  private fun resolveParameterForProvider(
    providerInfo: Pair<ProviderInfo, List<String>>,
    extensionContext: ExtensionContext,
    type: Class<*>
  ): Any {
    val pact = setupPactForTest(providerInfo.first, providerInfo.second, extensionContext)
    return if (type.isAssignableFrom(MockServer::class.java) && mockServerConfigured(extensionContext)) {
      setupMockServerForProvider(providerInfo.first, providerInfo.second, extensionContext)
    } else when (providerInfo.first.providerType) {
      ProviderType.ASYNCH -> when {
        type.isAssignableFrom(List::class.java) -> pact.interactions
        type.isAssignableFrom(V4Pact::class.java) -> pact.asV4Pact().unwrap()
        type.isAssignableFrom(MessagePact::class.java) -> pact.asMessagePact().unwrap()
        type.isAssignableFrom(V4Interaction.AsynchronousMessage::class.java) -> {
          val messages = pact.asV4Pact().unwrap().interactions.filter { it.isAsynchronousMessage() }
          if (messages.isEmpty()) {
            throw UnsupportedOperationException("Could not inject parameter $type into test method: no interactions " +
              "of type V4Interaction.AsynchronousMessage were found in the Pact")
          } else {
            if (messages.size > 1) {
              logger.warn { "More than one message was found in the Pact, using the first one" }
            }
            messages.first()
          }
        }
        else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
      }
      ProviderType.SYNCH_MESSAGE -> when {
        type.isAssignableFrom(List::class.java) -> pact.interactions
        type.isAssignableFrom(V4Pact::class.java) -> pact.asV4Pact().unwrap()
        type.isAssignableFrom(V4Interaction.SynchronousMessages::class.java) -> {
          val messages = pact.asV4Pact().unwrap().interactions.filter { it.isSynchronousMessages() }
          if (messages.isEmpty()) {
            throw UnsupportedOperationException("Could not inject parameter $type into test method: no interactions " +
              "of type V4Interaction.SynchronousMessages were found in the Pact")
          } else {
            if (messages.size > 1) {
              logger.warn { "More than one message was found in the Pact, using the first one" }
            }
            messages.first()
          }
        }
        else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
      }
      else -> when {
        type.isAssignableFrom(MockServer::class.java) ->
          setupMockServerForProvider(providerInfo.first, providerInfo.second, extensionContext)
        type.isAssignableFrom(RequestResponsePact::class.java) -> pact.asRequestResponsePact().unwrap()
        type.isAssignableFrom(V4Pact::class.java) -> pact.asV4Pact().unwrap()
        type.isAssignableFrom(V4Interaction.SynchronousHttp::class.java) -> {
          val interactions = pact.asV4Pact().unwrap().interactions.filter { it.isSynchronousRequestResponse() }
          if (interactions.isEmpty()) {
            throw UnsupportedOperationException("Could not inject parameter $type into test method: no interactions " +
              "of type V4Interaction.SynchronousHttp were found in the Pact")
          } else {
            if (interactions.size > 1) {
              logger.warn { "More than one interaction was found in the Pact, using the first one" }
            }
            interactions.first()
          }
        }
        type.isAssignableFrom(V4Interaction.SynchronousMessages::class.java) -> {
          val messages = pact.asV4Pact().unwrap().interactions.filter { it.isSynchronousMessages() }
          if (messages.isEmpty()) {
            throw UnsupportedOperationException("Could not inject parameter $type into test method: no interactions " +
              "of type V4Interaction.SynchronousMessages were found in the Pact")
          } else {
            if (messages.size > 1) {
              logger.warn { "More than one message was found in the Pact, using the first one" }
            }
            messages.first()
          }
        }
        else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
      }
    }
  }

  override fun beforeAll(context: ExtensionContext) {
    val store = context.getStore(NAMESPACE)
    store.put("executedFragments", ConcurrentHashMap.newKeySet<Method>())
    store.put("pactsToWrite", ConcurrentHashMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>())
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    if (!ignoredTest(context)) {
      for ((providerInfo, pactMethods) in lookupProviderInfo(context)) {
        logger.debug { "providerInfo = $providerInfo" }

        if (mockServerConfigured(context) ||
          providerInfo.providerType == null ||
          providerInfo.providerType == ProviderType.SYNCH ||
          providerInfo.providerType == ProviderType.UNSPECIFIED
        ) {
          val mockServer = setupMockServerForProvider(providerInfo, pactMethods, context)
          mockServer.start()
          mockServer.waitForServer()
        }
      }
    }
  }

  private fun ignoredTest(context: ExtensionContext): Boolean {
    return context.testMethod.isPresent &&
      AnnotationSupport.isAnnotated(context.testMethod.get(), PactIgnore::class.java)
  }

  private fun setupMockServerForProvider(
    providerInfo: ProviderInfo,
    pactMethods: List<String>,
    context: ExtensionContext
  ): AbstractBaseMockServer {
    val store = context.getStore(NAMESPACE)
    val key = "mockServer:${providerInfo.providerName}"
    return when {
      store[key] != null -> store[key] as AbstractBaseMockServer
      else -> {
        val config = mockServerConfigFromAnnotation(context, providerInfo).merge(providerInfo.mockServerConfig())
        store.put("mockServerConfig:${providerInfo.providerName}", config)
        val mockServer = mockServer(setupPactForTest(providerInfo, pactMethods, context), config)
        store.put(key, JUnit5MockServerSupport(mockServer))
        mockServer
      }
    }
  }

  private fun setupPactForTest(
    providerInfo: ProviderInfo,
    pactMethods: List<String>,
    context: ExtensionContext
  ): BasePact {
    val store = context.getStore(NAMESPACE)
    val key = "pact:${providerInfo.providerName}"
    return when {
      store[key] != null -> store[key] as BasePact
      else -> {
        val pact = if (pactMethods.isEmpty()) {
          lookupPact(providerInfo, "", context)
        } else {
          val head = pactMethods.first()
          val tail = pactMethods.drop(1)
          val initial = lookupPact(providerInfo, head, context)
          tail.fold(initial) { acc, method ->
            val pact = lookupPact(providerInfo, method, context)

            if (pact.provider != acc.provider) {
              // Should not really get here, as the Pacts should have been sorted by provider
              throw IllegalArgumentException("You are using different Pacts with different providers for the same test" +
                " ('${acc.provider}') and '${pact.provider}'). A separate test (and ideally a separate test class)" +
                " should be used for each provider.")
            }

            if (pact.consumer != acc.consumer) {
              logger.warn {
                "WARNING: You are using different Pacts with different consumers for the same test " +
                  "('${acc.consumer}') and '${pact.consumer}'). The second consumer will be ignored and dropped from " +
                  "the Pact and the interactions merged. If this is not your intention, you need to create a " +
                  "separate test for each consumer."
              }
            }

            acc.mergeInteractions(pact.interactions) as BasePact
          }
        }
        store.put(key, pact)
        pact
      }
    }
  }

  private fun mockServerConfigured(extensionContext: ExtensionContext): Boolean {
    val mockServerConfig = AnnotationSupport.findAnnotation(extensionContext.requiredTestClass,
      MockServerConfig::class.java)
    val mockServerConfigs = AnnotationSupport.findRepeatableAnnotations(extensionContext.requiredTestClass,
      MockServerConfig::class.java)
    val testMethod = extensionContext.testMethod
    val mockServerConfigMethod = if (testMethod.isPresent) {
      AnnotationSupport.findAnnotation(testMethod.get(), MockServerConfig::class.java)
    } else Optional.empty()
    val mockServerConfigMethods = if (testMethod.isPresent) {
      AnnotationSupport.findRepeatableAnnotations(testMethod.get(), MockServerConfig::class.java)
    } else emptyList()

    return mockServerConfig != null && mockServerConfig.isPresent ||
      mockServerConfigs.isNotEmpty() ||
      mockServerConfigMethod != null && mockServerConfigMethod.isPresent ||
      mockServerConfigMethods.isNotEmpty()
  }

  private fun mockServerConfigFromAnnotation(
    context: ExtensionContext,
    providerInfo: ProviderInfo?
  ): MockProviderConfig? {
    val mockServerConfigFromMethod = if (context.testMethod.isPresent)
      AnnotationSupport.findAnnotation(context.testMethod.get(), MockServerConfig::class.java)
    else null
    val mockServerConfigsFromMethod = if (context.testMethod.isPresent)
      AnnotationSupport.findRepeatableAnnotations(context.testMethod.get(), MockServerConfig::class.java)
    else emptyList()
    val mockServerConfig = AnnotationSupport.findAnnotation(context.requiredTestClass, MockServerConfig::class.java)
    val mockServerConfigs = AnnotationSupport.findRepeatableAnnotations(context.requiredTestClass,
      MockServerConfig::class.java)

    return when {
      mockServerConfigFromMethod != null && mockServerConfigFromMethod.isPresent ->
        MockProviderConfig.fromMockServerAnnotation(mockServerConfigFromMethod)

      mockServerConfig != null && mockServerConfig.isPresent ->
        MockProviderConfig.fromMockServerAnnotation(mockServerConfig)

      mockServerConfigsFromMethod.isNotEmpty() -> {
        val config = if (providerInfo != null) {
          Optional.ofNullable(mockServerConfigsFromMethod.firstOrNull { it.providerName == providerInfo.providerName })
        } else {
          Optional.ofNullable(mockServerConfigsFromMethod.firstOrNull())
        }
        MockProviderConfig.fromMockServerAnnotation(config)
      }

      mockServerConfigs.isNotEmpty() -> {
        val config = if (providerInfo != null) {
          Optional.ofNullable(mockServerConfigs.firstOrNull { it.providerName == providerInfo.providerName })
        } else {
          Optional.ofNullable(mockServerConfigs.firstOrNull())
        }
        MockProviderConfig.fromMockServerAnnotation(config)
      }

      else -> null
    }
  }

  fun lookupProviderInfo(context: ExtensionContext): List<Pair<ProviderInfo, List<String>>> {
    logger.trace { "lookupProviderInfo($context)" }
    val store = context.getStore(NAMESPACE)
    val providerInfo = when {
      store["providers"] != null -> store["providers"] as List<Pair<ProviderInfo, List<String>>>
      else -> {
        val methodAnnotation = pactTestForTestMethod(context)
        val classAnnotation = pactTestForClass(context)

        val providerInfo = when {
          classAnnotation != null && methodAnnotation != null ->
            ProviderInfo.fromAnnotation(methodAnnotation)
              .merge(ProviderInfo.fromAnnotation(classAnnotation))
          classAnnotation != null -> ProviderInfo.fromAnnotation(classAnnotation)
          methodAnnotation != null -> ProviderInfo.fromAnnotation(methodAnnotation)
          else -> {
            logger.warn { "No @PactTestFor annotation found on test class, using defaults" }
            null
          }
        }

        val providers = when {
          providerInfo != null -> {
            when {
              methodAnnotation != null -> if (methodAnnotation.pactMethods.isNotEmpty()) {
                buildProviderList(methodAnnotation, context, providerInfo)
              } else {
                val mockServerConfig = mockServerConfigFromAnnotation(context, providerInfo)
                val provider = providerInfo.withMockServerConfig(mockServerConfig)
                val pactMethods = if (methodAnnotation.pactMethod.isNotEmpty())
                  listOf(methodAnnotation.pactMethod)
                else emptyList()
                listOf(provider to pactMethods)
              }
              classAnnotation != null -> if (classAnnotation.pactMethods.isNotEmpty()) {
                buildProviderList(classAnnotation, context, providerInfo)
              } else {
                val mockServerConfig = mockServerConfigFromAnnotation(context, providerInfo)
                val provider = providerInfo.withMockServerConfig(mockServerConfig)
                val pactMethods = if (classAnnotation.pactMethod.isNotEmpty())
                  listOf(classAnnotation.pactMethod)
                else emptyList()
                listOf(provider to pactMethods)
              }
              else -> {
                logger.warn { "No @PactTestFor annotation found on test class, using defaults" }
                listOf(ProviderInfo() to listOf())
              }
            }
          }
          else -> {
            logger.warn { "No @PactTestFor annotation found on test class, using defaults" }
            listOf(ProviderInfo() to listOf())
          }
        }

        store.put("providers", providers)

        providers
      }
    }
    logger.trace { "providers = $providerInfo" }
    return providerInfo
  }

  private fun buildProviderList(
    annotation: PactTestFor,
    context: ExtensionContext,
    providerInfo: ProviderInfo
  ): List<Pair<ProviderInfo, MutableList<String>>> {
    val target = mutableMapOf<ProviderInfo, MutableList<String>>()
    return annotation.pactMethods.fold(target) { acc, method ->
      val providerName = providerNameFromPactMethod(method, context)
      val provider = if (providerName.isNotEmpty())
        providerInfo.copy(providerName = providerName)
      else providerInfo

      val key = acc.keys.firstOrNull { it.providerName == provider.providerName }
      if (key != null) {
        acc[key]!!.add(method)
      } else {
        val mockServerConfig = mockServerConfigFromAnnotation(context, provider)
        provider.withMockServerConfig(mockServerConfig)
        acc[provider] = mutableListOf(method)
      }
      acc
    }.toList()
  }

  private fun pactTestForClass(context: ExtensionContext) =
    if (AnnotationSupport.isAnnotated(context.requiredTestClass, PactTestFor::class.java)) {
      logger.debug { "Found @PactTestFor annotation on test ${context.requiredTestClass}" }
      AnnotationSupport.findAnnotation(context.requiredTestClass, PactTestFor::class.java).get()
    } else if (AnnotationSupport.isAnnotated(context.requiredTestClass, Nested::class.java)) {
      logger.debug {
        "Found @Nested annotation on test class ${context.requiredTestClass}, will search the enclosing classes"
      }
      val searchResult = Annotations.searchForAnnotation(context.requiredTestClass.kotlin, PactTestFor::class)
      if (searchResult != null) {
        logger.debug { "Found @PactTestFor annotation on outer $searchResult" }
        searchResult.findAnnotation()
      } else {
        null
      }
    } else {
      null
    }

  private fun pactTestForTestMethod(context: ExtensionContext) =
    if (context.testMethod.isPresent &&
      AnnotationSupport.isAnnotated(context.testMethod.get(), PactTestFor::class.java)) {
      val testMethod = context.testMethod.get()
      logger.debug { "Found @PactTestFor annotation on test method $testMethod" }
      AnnotationSupport.findAnnotation(testMethod, PactTestFor::class.java).get()
    } else {
      null
    }

  private fun providerNameFromPactMethod(methodName: String, context: ExtensionContext): String {
    val method = pactMethodAnnotation(null, context, methodName)
    return method!!.getAnnotation(Pact::class.java).provider
  }

  fun lookupPact(
    providerInfo: ProviderInfo,
    pactMethod: String,
    context: ExtensionContext
  ): BasePact {
    val store = context.getStore(NAMESPACE)
    val providerName = providerInfo.providerName.ifEmpty { "default" }
    val method = pactMethodAnnotation(providerName, context, pactMethod)

    val providerType = providerInfo.providerType ?: ProviderType.SYNCH
    if (method == null) {
      throw UnsupportedOperationException("No method annotated with @Pact was found on test class " +
        context.requiredTestClass.simpleName + " for provider '${providerInfo.providerName}'")
    } else if (providerType == ProviderType.SYNCH && !JUnitTestSupport.conformsToSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4)) {
      throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
        "'public [RequestResponsePact|V4Pact] xxx(PactBuilder builder)'")
    } else if (providerType == ProviderType.ASYNCH && !JUnitTestSupport.conformsToMessagePactSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4)) {
      throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
        "'public [MessagePact|V4Pact] xxx(PactBuilder builder)'")
    } else if (providerType == ProviderType.SYNCH_MESSAGE && !JUnitTestSupport.conformsToSynchMessagePactSignature(method, providerInfo.pactVersion ?: PactSpecVersion.V4)) {
      throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
        "'public V4Pact xxx(PactBuilder builder)'")
    }

    val pactAnnotation = AnnotationSupport.findAnnotation(method, Pact::class.java).get()
    val pactConsumer = ep.parseExpression(pactAnnotation.consumer, DataType.STRING)?.toString() ?: pactAnnotation.consumer
    logger.debug {
      "Invoking method '${method.name}' to get Pact for the test " +
        "'${context.testMethod.map { it.name }.orElse("unknown")}'"
    }

    val provider = ep.parseExpression(pactAnnotation.provider, DataType.STRING)?.toString()
    val providerNameToUse = if (provider.isNullOrEmpty()) providerName else provider
    val pact = when (providerType) {
      ProviderType.SYNCH, ProviderType.UNSPECIFIED -> {
        if (method.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.PactDslWithProvider"))) {
          val consumerPactBuilder = ConsumerPactBuilder.consumer(pactConsumer)
          if (providerInfo.pactVersion != null) {
            consumerPactBuilder.pactSpecVersion(providerInfo.pactVersion)
          }
          ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
            consumerPactBuilder.hasPactWith(providerNameToUse)) as BasePact
        } else {
          val pactBuilder = PactBuilder(pactConsumer, providerNameToUse)
          if (providerInfo.pactVersion != null) {
            pactBuilder.pactSpecVersion(providerInfo.pactVersion)
          }
          ReflectionSupport.invokeMethod(method, context.requiredTestInstance, pactBuilder) as BasePact
        }
      }
      ProviderType.ASYNCH -> {
        if (method.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.MessagePactBuilder"))) {
          ReflectionSupport.invokeMethod(
            method, context.requiredTestInstance,
            MessagePactBuilder(providerInfo.pactVersion ?: PactSpecVersion.V3)
              .consumer(pactConsumer).hasPactWith(providerNameToUse)
          ) as BasePact
        } else {
          val pactBuilder = PactBuilder(pactConsumer, providerNameToUse)
          if (providerInfo.pactVersion != null) {
            pactBuilder.pactSpecVersion(providerInfo.pactVersion)
          }
          ReflectionSupport.invokeMethod(method, context.requiredTestInstance, pactBuilder) as BasePact
        }
      }
      ProviderType.SYNCH_MESSAGE -> {
        if (method.parameterTypes[0].isAssignableFrom(Class.forName("au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder"))) {
          ReflectionSupport.invokeMethod(
            method, context.requiredTestInstance,
            SynchronousMessagePactBuilder(providerInfo.pactVersion ?: PactSpecVersion.V4)
              .consumer(pactConsumer).hasPactWith(providerNameToUse)
          ) as BasePact
        } else {
          val pactBuilder = PactBuilder(pactConsumer, providerNameToUse)
          if (providerInfo.pactVersion != null) {
            pactBuilder.pactSpecVersion(providerInfo.pactVersion)
          }
          ReflectionSupport.invokeMethod(method, context.requiredTestInstance, pactBuilder) as BasePact
        }
      }
    }

    if (providerInfo.pactVersion != null && providerInfo.pactVersion >= PactSpecVersion.V4) {
      pact.asV4Pact().unwrap().interactions.forEach { i ->
       i.comments["testname"] = Json.toJson(context.testClass.map { it.name + "." }.orElse("") +
         context.displayName)
      }
    }

    val executedFragments = store["executedFragments"] as MutableSet<Method>
    executedFragments.add(method)

    return pact
  }

  private fun pactMethodAnnotation(providerName: String?, context: ExtensionContext, pactMethod: String): Method? {
    val methods = AnnotationSupport.findAnnotatedMethods(context.requiredTestClass, Pact::class.java,
      HierarchyTraversalMode.TOP_DOWN)
    return when {
      pactMethod.isNotEmpty() -> {
        logger.debug { "Looking for @Pact method named '$pactMethod' for provider '$providerName'" }
        methods.firstOrNull { it.name == pactMethod }
      }
      providerName.isNullOrEmpty() -> {
        logger.debug { "Looking for first @Pact method" }
        methods.firstOrNull()
      }
      else -> {
        logger.debug { "Looking for first @Pact method for provider '$providerName'" }
        methods.firstOrNull {
          val pactAnnotationProviderName = AnnotationSupport.findAnnotation(it, Pact::class.java).get().provider
          val annotationProviderName = ep.parseExpression(pactAnnotationProviderName, DataType.STRING)?.toString()
            ?: pactAnnotationProviderName
          annotationProviderName.isEmpty() || annotationProviderName == providerName
        }
      }
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    if (!ignoredTest(context)) {
      val store = context.getStore(NAMESPACE)

      val providers = store["providers"] as List<Pair<ProviderInfo, String>>
      for ((provider, _) in providers) {
        val pact = store["pact:${provider.providerName}"] as BasePact?
        Metrics.sendMetrics(MetricEvent.ConsumerTestRun(pact?.interactions?.size ?: 0, "junit5"))
      }

      for ((provider, _) in providers) {
        if (store["mockServer:${provider.providerName}"] != null) {
          val mockServer = store["mockServer:${provider.providerName}"] as JUnit5MockServerSupport
          Thread.sleep(100) // give the mock server some time to have consistent state
          mockServer.close()
          val result = mockServer.validateMockServerState(null)
          if (result is PactVerificationResult.Ok) {
            if (!context.executionException.isPresent) {
              storePactForWrite(store, provider, mockServer)
            }
          } else {
            JUnitTestSupport.validateMockServerResult(result)
          }
        } else if (provider.providerType == ProviderType.ASYNCH || provider.providerType == ProviderType.SYNCH_MESSAGE) {
          if (!context.executionException.isPresent) {
            storePactForWrite(store, provider, null)
          }
        }
      }
    }
  }

  private fun storePactForWrite(
    store: ExtensionContext.Store,
    providerInfo: ProviderInfo,
    mockServer: MockServer?
  ) {
    @Suppress("UNCHECKED_CAST")
    val pactsToWrite = store["pactsToWrite"] as MutableMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>
    var pact = store["pact:${providerInfo.providerName}"] as BasePact
    val version = providerInfo.pactVersion ?: PactSpecVersion.V4

    if (mockServer != null) {
      pact = mockServer.updatePact(pact) as BasePact
    }

    pactsToWrite.merge(
      Pair(pact.consumer, pact.provider),
      Pair(pact, version)
    ) { (currentPact, currentVersion), _ ->
      val mergedPact = currentPact.mergeInteractions(pact.interactions) as BasePact
      Pair(mergedPact, maxOf(version, currentVersion))
    }
  }

  private fun lookupPactDirectory(context: ExtensionContext): String {
    logger.trace { "lookupPactDirectory($context)" }
    val pactFolder = AnnotationSupport.findAnnotation(context.requiredTestClass, PactFolder::class.java)
    val pactDirectory = if (AnnotationSupport.isAnnotated(context.requiredTestClass, Nested::class.java)) {
      val search = Annotations.searchForAnnotation(context.requiredTestClass.kotlin, PactDirectory::class)
      if (search != null) {
        Optional.of(search.findAnnotation()!!)
      } else {
        Optional.empty()
      }
    } else {
      AnnotationSupport.findAnnotation(context.requiredTestClass, PactDirectory::class.java)
    }
    return when {
      pactFolder.isPresent -> {
        logger.info { "Writing pacts out to directory from @PactFolder annotation" }
        logger.warn { "DEPRECATED: Annotation @PactFolder is deprecated and has been replaced with @PactDirectory" }
        pactFolder.get().value
      }
      pactDirectory.isPresent -> {
        logger.info { "Writing pacts out to directory from @PactDirectory annotation" }
        pactDirectory.get().value
      }
      else -> {
        logger.info { "Writing pacts out to default directory" }
        BuiltToolConfig.pactDirectory
      }
    }
  }

  override fun afterAll(context: ExtensionContext) {
    if (!context.executionException.isPresent) {
      val store = context.getStore(NAMESPACE)
      val pactDirectory = lookupPactDirectory(context)

      @Suppress("UNCHECKED_CAST")
      val pactsToWrite =
        store["pactsToWrite"] as MutableMap<Pair<Consumer, Provider>, Pair<BasePact, PactSpecVersion>>
      pactsToWrite.values
        .forEach { (pact, version) ->
          logger.debug {
            "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
              "${pact.fileForPact(pactDirectory)}"
          }
          pact.write(pactDirectory, version)
        }

      val executedFragments = store["executedFragments"] as MutableSet<Method>
      val methods = AnnotationSupport.findAnnotatedMethods(context.requiredTestClass, Pact::class.java,
        HierarchyTraversalMode.TOP_DOWN)
      if (executedFragments.size < methods.size) {
        val nonExecutedMethods = (methods - executedFragments).filter {
          !isAnnotated(it, Disabled::class.java)
        }.joinToString(", ") { it.declaringClass.simpleName + "." + it.name }
        if (nonExecutedMethods.isNotEmpty()) {
          throw AssertionError(
            "The following methods annotated with @Pact were not executed during the test: $nonExecutedMethods" +
              "\nIf these are currently a work in progress, add a @Disabled annotation to the method\n")
        }
      }
    }
  }

  companion object : KLogging() {
    val NAMESPACE: ExtensionContext.Namespace = ExtensionContext.Namespace.create("pact-jvm")
  }
}

fun MockProviderConfig?.merge(config: MockProviderConfig): MockProviderConfig {
  return this?.mergeWith(config) ?: config
}
