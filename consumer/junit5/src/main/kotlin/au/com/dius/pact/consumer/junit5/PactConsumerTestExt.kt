package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.AbstractBaseMockServer
import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactTestRun
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.junit.JUnitTestSupport
import au.com.dius.pact.consumer.mockServer
import au.com.dius.pact.consumer.model.MockHttpsProviderConfig
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.consumer.model.MockServerImplementation
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactFolder
import au.com.dius.pact.core.model.messaging.MessagePact
import au.com.dius.pact.core.support.expressions.DataType
import au.com.dius.pact.core.support.expressions.ExpressionParser.parseExpression
import mu.KLogging
import org.junit.jupiter.api.Disabled
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
import java.lang.annotation.Inherited
import java.lang.reflect.Method

/**
 * The type of provider (synchronous or asynchronous)
 */
enum class ProviderType {
  /**
   * Synchronous provider (HTTP)
   */
  SYNCH,
  /**
   * Asynchronous provider (Messages)
   */
  ASYNCH,
  /**
   * Unspecified, will default to synchronous
   */
  UNSPECIFIED
}

/**
 * Main test annotation for a JUnit 5 test
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class PactTestFor(
  /**
   * Providers name. This will be recorded in the pact file
   */
  val providerName: String = "",

  /**
   * Host interface to use for the mock server. Only used for synchronous provider tests and defaults to the
   * loopback adapter (127.0.0.1).
   */
  val hostInterface: String = "",

  /**
   * Port number to bind to. Only used for synchronous provider tests and defaults to 0, which causes a random free port to be chosen.
   */
  val port: String = "",

  /**
   * Pact specification version to support. Will default to V3.
   */
  val pactVersion: PactSpecVersion = PactSpecVersion.UNSPECIFIED,

  /**
   * Test method that provides the Pact to use for the test. Default behaviour is to use the first one found.
   */
  val pactMethod: String = "",

  /**
   * Type of provider (synchronous HTTP or asynchronous messages)
   */
  val providerType: ProviderType = ProviderType.UNSPECIFIED,

  /**
   * If HTTPS should be used. If enabled, a mock server with a self-signed cert will be started.
   */
  val https: Boolean = false,

  /**
   * The type of mock server implementation to use. The default is to use the Java server for HTTP and the KTor
   * server for HTTPS
   */
  val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default
)

data class ProviderInfo @JvmOverloads constructor(
  val providerName: String = "",
  val hostInterface: String = "",
  val port: String = "",
  val pactVersion: PactSpecVersion? = null,
  val providerType: ProviderType? = null,
  val https: Boolean = false,
  val mockServerImplementation: MockServerImplementation = MockServerImplementation.Default
) {
  fun mockServerConfig() = if (https) {
    MockHttpsProviderConfig.httpsConfig(
      if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
      if (port.isEmpty()) 0 else port.toInt(),
      pactVersion ?: PactSpecVersion.V3,
      mockServerImplementation
    )
  } else {
    MockProviderConfig.httpConfig(
      if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
      if (port.isEmpty()) 0 else port.toInt(),
      pactVersion ?: PactSpecVersion.V3,
      mockServerImplementation
    )
  }

  fun merge(other: ProviderInfo): ProviderInfo {
    return copy(providerName = if (providerName.isNotEmpty()) providerName else other.providerName,
      hostInterface = if (hostInterface.isNotEmpty()) hostInterface else other.hostInterface,
      port = if (port.isNotEmpty()) port else other.port,
      pactVersion = pactVersion ?: other.pactVersion,
      providerType = providerType ?: other.providerType,
      https = https || other.https,
      mockServerImplementation = mockServerImplementation.merge(other.mockServerImplementation)
    )
  }

  companion object {
    fun fromAnnotation(annotation: PactTestFor): ProviderInfo =
      ProviderInfo(annotation.providerName, annotation.hostInterface, annotation.port,
        when (annotation.pactVersion) {
          PactSpecVersion.UNSPECIFIED -> null
          else -> annotation.pactVersion
        },
        when (annotation.providerType) {
          ProviderType.UNSPECIFIED -> null
          else -> annotation.providerType
        }, annotation.https, annotation.mockServerImplementation)
  }
}

class JUnit5MockServerSupport(private val baseMockServer: BaseMockServer) : AbstractBaseMockServer(),
  ExtensionContext.Store.CloseableResource {
  override fun close() {
    baseMockServer.stop()
  }

  override fun start() = baseMockServer.start()
  override fun stop() = baseMockServer.stop()
  override fun waitForServer() = baseMockServer.waitForServer()
  override fun getUrl() = baseMockServer.getUrl()
  override fun getPort() = baseMockServer.getPort()
  override fun <R> runAndWritePact(pact: RequestResponsePact, pactVersion: PactSpecVersion, testFn: PactTestRun<R>) =
    baseMockServer.runAndWritePact(pact, pactVersion, testFn)
  override fun validateMockServerState(testResult: Any?) = baseMockServer.validateMockServerState(testResult)
}

class PactConsumerTestExt : Extension, BeforeTestExecutionCallback, BeforeAllCallback, ParameterResolver, AfterTestExecutionCallback, AfterAllCallback {
  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val providerInfo = lookupProviderInfo(extensionContext).first
    val type = parameterContext.parameter.type
    return when (providerInfo.providerType) {
      ProviderType.ASYNCH -> when {
        type.isAssignableFrom(List::class.java) -> true
        type.isAssignableFrom(MessagePact::class.java) -> true
        else -> false
      }
      else -> when {
        type.isAssignableFrom(MockServer::class.java) -> true
        type.isAssignableFrom(RequestResponsePact::class.java) -> true
        else -> false
      }
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
    val providerInfo = lookupProviderInfo(extensionContext)
    val store = extensionContext.getStore(NAMESPACE)
    val type = parameterContext.parameter.type
    return when (providerInfo.first.providerType) {
      ProviderType.ASYNCH -> {
        val pact = lookupPact(providerInfo.first, providerInfo.second, extensionContext) as MessagePact
        when {
          type.isAssignableFrom(List::class.java) -> pact.messages
          type.isAssignableFrom(MessagePact::class.java) -> pact
          else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
        }
      }
      else -> {
        when {
          type.isAssignableFrom(MockServer::class.java) -> setupMockServer(providerInfo.first, providerInfo.second, extensionContext)!!
          type.isAssignableFrom(RequestResponsePact::class.java) -> store["pact"] as RequestResponsePact
          else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
        }
      }
    }
  }

  override fun beforeAll(context: ExtensionContext) {
    val store = context.getStore(NAMESPACE)
    store.put("executedFragments", mutableListOf<Method>())
    store.put("pactsToWrite", mutableMapOf<Pair<Consumer, Provider>, Pair<BasePact<*>, PactSpecVersion>>())
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    val (providerInfo, pactMethod) = lookupProviderInfo(context)
    logger.debug { "providerInfo = $providerInfo" }

    if (providerInfo.providerType != ProviderType.ASYNCH) {
      val mockServer = setupMockServer(providerInfo, pactMethod, context)
      mockServer.start()
      mockServer.waitForServer()
    }
  }

  private fun setupMockServer(providerInfo: ProviderInfo, pactMethod: String, context: ExtensionContext): AbstractBaseMockServer {
    val store = context.getStore(NAMESPACE)
    return if (store["mockServer"] == null) {
      val config = providerInfo.mockServerConfig()

      store.put("mockServerConfig", config)
      val mockServer = mockServer(lookupPact(providerInfo, pactMethod, context) as RequestResponsePact, config)
      store.put("mockServer", JUnit5MockServerSupport(mockServer))
      mockServer
    } else {
      store["mockServer"] as AbstractBaseMockServer
    }
  }

  fun lookupProviderInfo(context: ExtensionContext): Pair<ProviderInfo, String> {
    val store = context.getStore(NAMESPACE)
    return if (store["providerInfo"] != null) {
      (store["providerInfo"] as ProviderInfo) to store["pactMethod"].toString()
    } else {
      val methodAnnotation = if (AnnotationSupport.isAnnotated(context.requiredTestMethod, PactTestFor::class.java)) {
        logger.debug { "Found @PactTestFor annotation on test method" }
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestMethod, PactTestFor::class.java).get()
        ProviderInfo.fromAnnotation(annotation) to annotation.pactMethod
      } else {
        null
      }

      val classAnnotation = if (AnnotationSupport.isAnnotated(context.requiredTestClass, PactTestFor::class.java)) {
        logger.debug { "Found @PactTestFor annotation on test class" }
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestClass, PactTestFor::class.java).get()
        ProviderInfo.fromAnnotation(annotation) to annotation.pactMethod
      } else {
        null
      }

      val providerInfo = when {
        classAnnotation != null && methodAnnotation != null -> Pair(methodAnnotation.first.merge(classAnnotation.first),
          if (methodAnnotation.second.isNotEmpty()) methodAnnotation.second else classAnnotation.second)
        classAnnotation != null -> classAnnotation
        methodAnnotation != null -> methodAnnotation
        else -> {
          logger.debug { "No @PactTestFor annotation found on test class, using defaults" }
          ProviderInfo() to ""
        }
      }

      store.put("providerInfo", providerInfo.first)
      store.put("pactMethod", providerInfo.second)

      providerInfo
    }
  }

  fun lookupPact(
    providerInfo: ProviderInfo,
    pactMethod: String,
    context: ExtensionContext
  ): BasePact<out Interaction> {
    val store = context.getStore(NAMESPACE)
    if (store["pact"] == null) {
      val providerName = if (providerInfo.providerName.isEmpty()) "default" else providerInfo.providerName
      val methods = AnnotationSupport.findAnnotatedMethods(context.requiredTestClass, Pact::class.java,
        HierarchyTraversalMode.TOP_DOWN)

      val method = when {
        pactMethod.isNotEmpty() -> {
          logger.debug { "Looking for @Pact method named '$pactMethod' for provider '$providerName'" }
          methods.firstOrNull { it.name == pactMethod }
        }
        providerInfo.providerName.isEmpty() -> {
          logger.debug { "Looking for first @Pact method" }
          methods.firstOrNull()
        }
        else -> {
          logger.debug { "Looking for first @Pact method for provider '$providerName'" }
          methods.firstOrNull {
            val annotationProviderName = AnnotationSupport.findAnnotation(it, Pact::class.java).get().provider
            annotationProviderName.isEmpty() || annotationProviderName == providerInfo.providerName
          }
        }
      }

      val providerType = providerInfo.providerType ?: ProviderType.SYNCH
      if (method == null) {
        throw UnsupportedOperationException("No method annotated with @Pact was found on test class " +
          context.requiredTestClass.simpleName + " for provider '${providerInfo.providerName}'")
      } else if (providerType == ProviderType.SYNCH && !JUnitTestSupport.conformsToSignature(method)) {
        throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
          "'public RequestResponsePact xxx(PactDslWithProvider builder)'")
      } else if (providerType == ProviderType.ASYNCH && !JUnitTestSupport.conformsToMessagePactSignature(method)) {
        throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
          "'public MessagePact xxx(MessagePactBuilder builder)'")
      }

      val pactAnnotation = AnnotationSupport.findAnnotation(method, Pact::class.java).get()
      logger.debug {
        "Invoking method '${method.name}' to get Pact for the test " +
          "'${context.testMethod.map { it.name }.orElse("unknown")}'"
      }

      val provider = parseExpression(pactAnnotation.provider, DataType.RAW)?.toString()
      val providerNameToUse = if (provider.isNullOrEmpty()) providerName else provider
      val pact = when (providerType) {
        ProviderType.SYNCH, ProviderType.UNSPECIFIED -> ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
          ConsumerPactBuilder.consumer(pactAnnotation.consumer).hasPactWith(providerNameToUse)) as BasePact<*>
        ProviderType.ASYNCH -> ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
          MessagePactBuilder.consumer(pactAnnotation.consumer).hasPactWith(providerNameToUse)) as BasePact<*>
      }
      val executedFragments = store["executedFragments"] as MutableList<Method>
      executedFragments.add(method)
      store.put("pact", pact)
      return pact
    } else {
      return store["pact"] as BasePact<out Interaction>
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    if (!context.executionException.isPresent) {
      val store = context.getStore(NAMESPACE)
      val providerInfo = store["providerInfo"] as ProviderInfo
      if (providerInfo.providerType == ProviderType.ASYNCH) {
        storePactForWrite(store)
      } else {
        val mockServer = store["mockServer"] as JUnit5MockServerSupport
        Thread.sleep(100) // give the mock server some time to have consistent state
        mockServer.close()
        val result = mockServer.validateMockServerState(null)
        if (result is PactVerificationResult.Ok) {
          storePactForWrite(store)
        } else {
          JUnitTestSupport.validateMockServerResult(result)
        }
      }
    }
  }

  private fun storePactForWrite(store: ExtensionContext.Store) {
    @Suppress("UNCHECKED_CAST")
    val pactsToWrite = store["pactsToWrite"] as MutableMap<Pair<Consumer, Provider>, Pair<BasePact<*>, PactSpecVersion>>
    val pact = store["pact"] as BasePact<*>
    val providerInfo = store["providerInfo"] as ProviderInfo
    val version = providerInfo.pactVersion ?: PactSpecVersion.V3

    pactsToWrite.merge(
      Pair(pact.consumer, pact.provider),
      Pair(pact, version)
    ) { (currentPact, currentVersion), _ ->
      currentPact.mergeInteractions(pact.interactions)
      Pair(currentPact, maxOf(version, currentVersion))
    }
  }

  private fun lookupPactDirectory(context: ExtensionContext): String {
    val pactFolder = AnnotationSupport.findAnnotation(context.requiredTestClass, PactFolder::class.java)
    return if (pactFolder.isPresent)
      pactFolder.get().value
    else
      PactConsumerConfig.pactDirectory
  }

  override fun afterAll(context: ExtensionContext) {
    if (!context.executionException.isPresent) {
      val store = context.getStore(NAMESPACE)
      val pactDirectory = lookupPactDirectory(context)

      @Suppress("UNCHECKED_CAST")
      val pactsToWrite =
        store["pactsToWrite"] as MutableMap<Pair<Consumer, Provider>, Pair<BasePact<*>, PactSpecVersion>>
      pactsToWrite.values
        .forEach { (pact, version) ->
          logger.debug {
            "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
              "${pact.fileForPact(pactDirectory)}"
          }
          pact.write(pactDirectory, version)
        }

      val executedFragments = store["executedFragments"] as MutableList<Method>
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
    val NAMESPACE = ExtensionContext.Namespace.create("pact-jvm")
  }
}
