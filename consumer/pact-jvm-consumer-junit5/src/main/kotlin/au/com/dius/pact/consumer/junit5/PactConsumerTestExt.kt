package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.BaseMockServer
import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.consumer.junit.JUnitTestSupport
import au.com.dius.pact.consumer.mockServer
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.BasePact
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactFolder
import au.com.dius.pact.core.model.messaging.MessagePact
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
  val providerType: ProviderType = ProviderType.UNSPECIFIED
)

data class ProviderInfo @JvmOverloads constructor(
  val providerName: String = "",
  val hostInterface: String = "",
  val port: String = "",
  val pactVersion: PactSpecVersion? = null,
  val providerType: ProviderType? = null
) {

  fun mockServerConfig() =
    MockProviderConfig.httpConfig(if (hostInterface.isEmpty()) MockProviderConfig.LOCALHOST else hostInterface,
      if (port.isEmpty()) 0 else port.toInt(), pactVersion ?: PactSpecVersion.V3)

  fun merge(other: ProviderInfo): ProviderInfo {
    return copy(providerName = if (providerName.isNotEmpty()) providerName else other.providerName,
      hostInterface = if (hostInterface.isNotEmpty()) hostInterface else other.hostInterface,
      port = if (port.isNotEmpty()) port else other.port,
      pactVersion = pactVersion ?: other.pactVersion,
      providerType = providerType ?: other.providerType)
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
        })
  }
}

class JUnit5MockServerSupport(private val baseMockServer: BaseMockServer) : MockServer by baseMockServer,
  ExtensionContext.Store.CloseableResource {
  override fun close() {
    baseMockServer.stop()
  }
}

class PactConsumerTestExt : Extension, BeforeTestExecutionCallback, BeforeAllCallback, ParameterResolver, AfterTestExecutionCallback, AfterAllCallback {
  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    if (store["providerInfo"] != null) {
      val providerInfo = store["providerInfo"] as ProviderInfo
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
    } else {
      return false
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    if (store["providerInfo"] != null) {
      val providerInfo = store["providerInfo"] as ProviderInfo
      val type = parameterContext.parameter.type
      return when (providerInfo.providerType) {
        ProviderType.ASYNCH -> {
          val pact = store["pact"] as MessagePact
          when {
            type.isAssignableFrom(List::class.java) -> pact.messages
            type.isAssignableFrom(MessagePact::class.java) -> pact
            else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
          }
        }
        else -> {
          val pact = store["pact"] as RequestResponsePact
          when {
            type.isAssignableFrom(MockServer::class.java) -> store["mockServer"]
            type.isAssignableFrom(RequestResponsePact::class.java) -> pact
            else -> throw UnsupportedOperationException("Could not inject parameter $type into test method")
          }
        }
      }
    } else {
      return false
    }
  }

  override fun beforeAll(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    store.put("executedFragments", mutableListOf<Method>())
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    val (providerInfo, pactMethod) = lookupProviderInfo(context)

    logger.debug { "providerInfo = $providerInfo" }

    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val executedFragments = store["executedFragments"] as MutableList<Method>
    val pact = lookupPact(providerInfo, pactMethod, context, executedFragments)
    store.put("pact", pact)
    store.put("providerInfo", providerInfo)

    if (providerInfo.providerType != ProviderType.ASYNCH) {
      val config = providerInfo.mockServerConfig()
      store.put("mockServerConfig", config)
      val mockServer = mockServer(pact as RequestResponsePact, config) as BaseMockServer
      mockServer.start()
      mockServer.waitForServer()
      store.put("mockServer", JUnit5MockServerSupport(mockServer))
    }
  }

  fun lookupProviderInfo(context: ExtensionContext): Pair<ProviderInfo, String> {
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

    return when {
      classAnnotation != null && methodAnnotation != null -> Pair(methodAnnotation.first.merge(classAnnotation.first),
        if (methodAnnotation.second.isNotEmpty()) methodAnnotation.second else classAnnotation.second)
      classAnnotation != null -> classAnnotation
      methodAnnotation != null -> methodAnnotation
      else -> {
        logger.debug { "No @PactTestFor annotation found on test class, using defaults" }
        ProviderInfo() to ""
      }
    }
  }

  fun lookupPact(
    providerInfo: ProviderInfo,
    pactMethod: String,
    context: ExtensionContext,
    executedFragments: MutableList<Method>
  ): BasePact<out Interaction> {
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
    logger.debug { "Invoking method '${method.name}' to get Pact for the test " +
      "'${context.testMethod.map { it.name }.orElse("unknown")}'" }

    val provider = parseExpression(pactAnnotation.provider).toString()
    val providerNameToUse = if (provider.isNotEmpty()) provider else providerName
    val pact = when (providerType) {
      ProviderType.SYNCH, ProviderType.UNSPECIFIED -> ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
        ConsumerPactBuilder.consumer(pactAnnotation.consumer).hasPactWith(providerNameToUse)) as BasePact<*>
      ProviderType.ASYNCH -> ReflectionSupport.invokeMethod(method, context.requiredTestInstance,
        MessagePactBuilder.consumer(pactAnnotation.consumer).hasPactWith(providerNameToUse)) as BasePact<*>
    }
    executedFragments.add(method)
    return pact
  }

  override fun afterTestExecution(context: ExtensionContext) {
    if (!context.executionException.isPresent) {
      val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
      val providerInfo = store["providerInfo"] as ProviderInfo
      val pactDirectory = lookupPactDirectory(context)
      if (providerInfo.providerType != ProviderType.ASYNCH) {
        val mockServer = store["mockServer"] as JUnit5MockServerSupport
        val pact = store["pact"] as RequestResponsePact
        val config = store["mockServerConfig"] as MockProviderConfig
        Thread.sleep(100) // give the mock server some time to have consistent state
        mockServer.close()
        val result = mockServer.validateMockServerState(null)
        if (result is PactVerificationResult.Ok) {
          logger.debug {
            "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
              "${pact.fileForPact(pactDirectory)}"
          }
          val pactFile = pact.fileForPact(pactDirectory)
          DefaultPactWriter.writePact(pactFile, pact, config.pactVersion)
        } else {
          JUnitTestSupport.validateMockServerResult(result)
        }
      } else {
        val pact = store["pact"] as MessagePact
        logger.debug {
          "Writing pact ${pact.consumer.name} -> ${pact.provider.name} to file " +
            "${pact.fileForPact(pactDirectory)}"
        }
        pact.write(pactDirectory, PactSpecVersion.V3)
      }
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
      val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
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
              "\nIf these are currently a work in progress, and a @Disabled annotation to the method\n")
        }
      }
    }
  }

  companion object : KLogging()
}
