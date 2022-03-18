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
import com.github.michaelbull.result.unwrap
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

class PactConsumerTestExt : Extension, BeforeTestExecutionCallback, BeforeAllCallback, ParameterResolver, AfterTestExecutionCallback, AfterAllCallback {

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
    providerInfo: Pair<ProviderInfo, String>,
    extensionContext: ExtensionContext,
    type: Class<*>
  ): Any {
    val pact = lookupPact(providerInfo.first, providerInfo.second, extensionContext)
    return if (type.isAssignableFrom(MockServer::class.java) && mockServerConfigured(extensionContext)) {
      setupMockServer(providerInfo.first, providerInfo.second, extensionContext)
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
        type.isAssignableFrom(MockServer::class.java) -> setupMockServer(providerInfo.first, providerInfo.second, extensionContext)
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
    for ((providerInfo, pactMethod) in lookupProviderInfo(context)) {
      logger.debug { "providerInfo = $providerInfo" }

      if (mockServerConfigured(context) ||
        providerInfo.providerType == null ||
        providerInfo.providerType == ProviderType.SYNCH ||
        providerInfo.providerType == ProviderType.UNSPECIFIED) {
        val mockServer = setupMockServer(providerInfo, pactMethod, context)
        mockServer.start()
        mockServer.waitForServer()
      }
    }
  }

  private fun setupMockServer(providerInfo: ProviderInfo, pactMethod: String, context: ExtensionContext): AbstractBaseMockServer {
    val store = context.getStore(NAMESPACE)
    val key = "mockServer:${providerInfo.providerName}"
    return when {
      store[key] != null -> store[key] as AbstractBaseMockServer
      else -> {
        val config = mockServerConfigFromAnnotation(context) ?: providerInfo.mockServerConfig()
        store.put("mockServerConfig:${providerInfo.providerName}", config)
        val mockServer = mockServer(lookupPact(providerInfo, pactMethod, context), config)
        store.put(key, JUnit5MockServerSupport(mockServer))
        mockServer
      }
    }
  }

  private fun mockServerConfigured(extensionContext: ExtensionContext): Boolean {
    val mockServerConfig = AnnotationSupport.findAnnotation(extensionContext.requiredTestClass,
      MockServerConfig::class.java)
    val mockServerConfigMethod = AnnotationSupport.findAnnotation(extensionContext.requiredTestMethod,
      MockServerConfig::class.java)
    return mockServerConfig != null && mockServerConfig.isPresent ||
      mockServerConfigMethod != null && mockServerConfigMethod.isPresent
  }

  private fun mockServerConfigFromAnnotation(context: ExtensionContext): MockProviderConfig? {
    val mockServerConfigMethod = AnnotationSupport.findAnnotation(context.requiredTestMethod,
      MockServerConfig::class.java)
    return if (mockServerConfigMethod != null) {
      MockProviderConfig.fromMockServerAnnotation(mockServerConfigMethod)
    } else {
      val mockServerConfig = AnnotationSupport.findAnnotation(context.requiredTestClass,
        MockServerConfig::class.java)
      if (mockServerConfig != null) {
        MockProviderConfig.fromMockServerAnnotation(mockServerConfig)
      } else {
        null
      }
    }
  }

  fun lookupProviderInfo(context: ExtensionContext): List<Pair<ProviderInfo, String>> {
    logger.trace { "lookupProviderInfo($context)" }
    val store = context.getStore(NAMESPACE)
    val providerInfo = when {
      store["providers"] != null -> store["providers"] as List<Pair<ProviderInfo, String>>
      else -> {
        val methodAnnotation = if (AnnotationSupport.isAnnotated(context.requiredTestMethod, PactTestFor::class.java)) {
          logger.debug { "Found @PactTestFor annotation on test method ${context.requiredTestMethod}" }
          AnnotationSupport.findAnnotation(context.requiredTestMethod, PactTestFor::class.java).get()
        } else {
          null
        }

        val classAnnotation = if (AnnotationSupport.isAnnotated(context.requiredTestClass, PactTestFor::class.java)) {
          logger.debug { "Found @PactTestFor annotation on test ${context.requiredTestClass}" }
          AnnotationSupport.findAnnotation(context.requiredTestClass, PactTestFor::class.java).get()
        } else if (AnnotationSupport.isAnnotated(context.requiredTestClass, Nested::class.java)) {
          logger.debug { "Found @Nested annotation on test class ${context.requiredTestClass}, will search the enclosing classes" }
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

        val providers = when {
          classAnnotation != null && methodAnnotation != null -> {
            val provider = ProviderInfo.fromAnnotation(methodAnnotation)
              .merge(ProviderInfo.fromAnnotation(classAnnotation))
            when {
              methodAnnotation.pactMethods.isNotEmpty() -> {
                methodAnnotation.pactMethods.map {
                  val providerName = providerNameFromPactMethod(it, context)
                  provider.copy(providerName = providerName) to it
                }
              }
              classAnnotation.pactMethods.isNotEmpty() -> {
                classAnnotation.pactMethods.map {
                  val providerName = providerNameFromPactMethod(it, context)
                  provider.copy(providerName = providerName) to it
                }
              }
              else -> listOf(provider to methodAnnotation.pactMethod.ifEmpty { classAnnotation.pactMethod })
            }
          }
          classAnnotation != null -> if (classAnnotation.pactMethods.isNotEmpty()) {
            val annotation = ProviderInfo.fromAnnotation(classAnnotation)
            classAnnotation.pactMethods.map {
              val providerName = providerNameFromPactMethod(it, context)
              annotation.copy(providerName = providerName) to it
            }
          } else {
            listOf(ProviderInfo.fromAnnotation(classAnnotation) to classAnnotation.pactMethod)
          }
          methodAnnotation != null -> if (methodAnnotation.pactMethods.isNotEmpty()) {
            val annotation = ProviderInfo.fromAnnotation(methodAnnotation)
            methodAnnotation.pactMethods.map {
              val providerName = providerNameFromPactMethod(it, context)
              annotation.copy(providerName = providerName) to it
            }
          } else {
            listOf(ProviderInfo.fromAnnotation(methodAnnotation) to methodAnnotation.pactMethod)
          }
          else -> {
            logger.warn { "No @PactTestFor annotation found on test class, using defaults" }
            listOf(ProviderInfo() to "")
          }
        }

        store.put("providers", providers)

        providers
      }
    }
    logger.trace { "providers = $providerInfo" }
    return providerInfo
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
    if (store["pact:${providerInfo.providerName}"] == null) {
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
      store.put("pact:${providerInfo.providerName}", pact)
      return pact
    } else {
      return store["pact:${providerInfo.providerName}"] as BasePact
    }
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
      Pair(pact as BasePact, version)
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
