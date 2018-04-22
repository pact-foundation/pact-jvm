package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.Pact
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.junit.JUnitTestSupport
import au.com.dius.pact.consumer.mockServer
import au.com.dius.pact.consumer.pactDirectory
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.RequestResponsePact
import mu.KLogging
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class PactTestFor(val providerName: String = "",
                             val hostInterface: String = "localhost",
                             val port: String = "8080",
                             val pactVersion: PactSpecVersion = PactSpecVersion.V3,
                             val pactMethod: String = "")

data class ProviderInfo(val providerName: String = "",
                        val hostInterface: String = "localhost",
                        val port: String = "8080",
                        val pactVersion: PactSpecVersion = PactSpecVersion.V3) {

  fun mockServerConfig() =
    MockProviderConfig.httpConfig(if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
      if (port.isEmpty()) 0 else port.toInt(), pactVersion)

  companion object {
    fun fromAnnotation(annotation: PactTestFor): ProviderInfo =
      ProviderInfo(annotation.providerName, annotation.hostInterface, annotation.port, annotation.pactVersion)
  }
}

class JUnit5MockServerSupport(private val baseMockServer: BaseMockServer) : MockServer by baseMockServer,
  ExtensionContext.Store.CloseableResource {
  override fun close() {
    baseMockServer.stop()
  }
}

class PactConsumerTestExt : Extension, BeforeEachCallback, ParameterResolver, AfterEachCallback {

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
    parameterContext.parameter.type.isAssignableFrom(MockServer::class.java)

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    return store["mockServer"]
  }

  override fun beforeEach(context: ExtensionContext) {
    val (providerInfo, pactMethod) = when {
      AnnotationSupport.isAnnotated(context.requiredTestMethod, PactTestFor::class.java) -> {
        logger.debug { "Found @PactTestFor annotation on test method" }
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestMethod, PactTestFor::class.java).get()
        ProviderInfo.fromAnnotation(annotation) to annotation.pactMethod
      }
      AnnotationSupport.isAnnotated(context.requiredTestClass, PactTestFor::class.java) -> {
        logger.debug { "Found @PactTestFor annotation on test class" }
        val annotation = AnnotationSupport.findAnnotation(context.requiredTestClass, PactTestFor::class.java).get()
        ProviderInfo.fromAnnotation(annotation) to annotation.pactMethod
      }
      else -> {
        logger.debug { "No @PactTestFor annotation found on test class, using defaults" }
        ProviderInfo() to ""
      }
    }

    val pact = lookupPact(providerInfo, pactMethod, context)
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    store.put("pact", pact)
    val config = providerInfo.mockServerConfig()
    store.put("mockServerConfig", config)
    val mockServer = mockServer(pact, config) as BaseMockServer
    mockServer.start()
    mockServer.waitForServer()
    store.put("mockServer", JUnit5MockServerSupport(mockServer))
  }

  fun lookupPact(providerInfo: ProviderInfo, pactMethod: String, context: ExtensionContext): RequestResponsePact {
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
          AnnotationSupport.findAnnotation(it, Pact::class.java).get().provider == providerInfo.providerName
        }
      }
    }

    if (method == null) {
      throw UnsupportedOperationException("No method annotated with @Pact was found on test class " +
        context.requiredTestClass.simpleName + " for provider '${providerInfo.providerName}'")
    } else if (!JUnitTestSupport.conformsToSignature(method)) {
      throw UnsupportedOperationException("Method ${method.name} does not conform to required method signature " +
        "'public RequestResponsePact xxx(PactDslWithProvider builder)'")
    }

    val pactAnnotation = AnnotationSupport.findAnnotation(method, Pact::class.java).get()
    logger.debug { "Invoking method '${method.name}' to get Pact for the test " +
      "'${context.testMethod.map { it.name }.orElse("unknown")}'" }
    return ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
      ConsumerPactBuilder.consumer(pactAnnotation.consumer).hasPactWith(pactAnnotation.provider)) as RequestResponsePact
  }

  override fun afterEach(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val mockServer = store["mockServer"] as JUnit5MockServerSupport
    val pact = store["pact"] as RequestResponsePact
    val config = store["mockServerConfig"] as MockProviderConfig
    Thread.sleep(100) // give the mock server some time to have consistent state
    mockServer.close()
    val result = mockServer.validateMockServerState()
    if (result === PactVerificationResult.Ok) {
      val pactDirectory = pactDirectory()
      logger.debug { "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
        "${pact.fileForPact(pactDirectory)}" }
      pact.write(pactDirectory, config.pactVersion)
    } else {
      JUnitTestSupport.validateMockServerResult(result)
    }
  }

  companion object : KLogging()
}
