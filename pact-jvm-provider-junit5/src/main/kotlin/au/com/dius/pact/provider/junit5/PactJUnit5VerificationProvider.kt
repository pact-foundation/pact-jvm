package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.pactbroker.TestResult
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.ProviderVerifierBase
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.junit.Consumer
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junit.MissingStateChangeMethod
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.StateChangeAction
import au.com.dius.pact.provider.junit.VerificationReports
import au.com.dius.pact.provider.junit.loader.PactLoader
import au.com.dius.pact.provider.junit.loader.PactSource
import au.com.dius.pact.provider.reporters.ReporterManager
import au.com.dius.pact.support.expressions.SystemPropertyResolver
import au.com.dius.pact.support.expressions.ValueResolver
import mu.KLogging
import org.apache.http.HttpRequest
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.support.HierarchyTraversalMode
import org.junit.platform.commons.support.ReflectionSupport
import java.io.File
import java.lang.reflect.Method
import java.util.stream.Stream
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

/**
 * The instance that holds the context for the test of an interaction. The test target will need to be set on it in
 * the before each phase of the test, and the verifyInteraction method must be called in the test template method.
 */
data class PactVerificationContext @JvmOverloads constructor(
  private val store: ExtensionContext.Store,
  private val context: ExtensionContext,
  var target: TestTarget = HttpTestTarget(port = 8080),
  var verifier: IProviderVerifier? = null,
  var valueResolver: ValueResolver = SystemPropertyResolver(),
  var providerInfo: ProviderInfo = ProviderInfo(),
  val consumerName: String,
  val interaction: Interaction,
  internal var testExecutionResult: TestResult = TestResult.Ok
) {
  val stateChangeHandlers: MutableList<Any> = mutableListOf()
  var executionContext: Map<String, Any?>? = null

  /**
   * Called to verify the interaction from the test template method.
   *
   * @throws AssertionError Throws an assertion error if the verification fails.
   */
  fun verifyInteraction() {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val client = store.get("client")
    val request = store.get("request")
    val failures = mutableMapOf<String, Any>()
    try {
      this.testExecutionResult = validateTestExecution(client, request, failures)
      if (testExecutionResult is TestResult.Failed) {
        verifier!!.displayFailures(failures)
        throw AssertionError(JUnitProviderTestSupport.generateErrorStringFromMismatches(failures))
      }
    } finally {
      verifier!!.finaliseReports()
    }
  }

  private fun validateTestExecution(client: Any?, request: Any?, failures: MutableMap<String, Any>): TestResult {
    if (providerInfo.verificationType == null || providerInfo.verificationType == PactVerification.REQUEST_RESPONSE) {
      val interactionMessage = "Verifying a pact between $consumerName and ${providerInfo.name}" +
        " - ${interaction.description}"
      return try {
        val reqResInteraction = interaction as RequestResponseInteraction
        val expectedResponse = reqResInteraction.response
        val actualResponse = target.executeInteraction(client, request)

        verifier!!.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures,
          reqResInteraction.interactionId.orEmpty())
      } catch (e: Exception) {
        failures[interactionMessage] = e
        verifier!!.reporters.forEach {
          it.requestFailed(providerInfo, interaction, interactionMessage, e,
            verifier!!.projectHasProperty.apply(ProviderVerifierBase.PACT_SHOW_STACKTRACE))
        }
        TestResult.Failed(listOf(mapOf("message" to "Request to provider failed with an exception",
          "exception" to e, "interactionId" to interaction.interactionId)),
          "Request to provider failed with an exception")
      }
    } else {
      return verifier!!.verifyResponseByInvokingProviderMethods(providerInfo, ConsumerInfo(consumerName), interaction,
        interaction.description, failures)
    }
  }

  fun withStateChangeHandlers(vararg stateClasses: Any): PactVerificationContext {
    stateChangeHandlers.addAll(stateClasses)
    return this
  }

  fun addStateChangeHandlers(vararg stateClasses: Any) {
    stateChangeHandlers.addAll(stateClasses)
  }
}

/**
 * JUnit 5 test extension class used to inject parameters and execute the test for a Pact interaction.
 */
class PactVerificationExtension(
  private val pact: Pact<Interaction>,
  private val pactSource: au.com.dius.pact.model.PactSource,
  private val interaction: Interaction,
  private val serviceName: String,
  private val consumerName: String?
) : TestTemplateInvocationContext, ParameterResolver, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback {

  private val testResultAccumulator = DefaultTestResultAccumulator

  override fun getDisplayName(invocationIndex: Int): String {
    return "${pact.consumer.name} - ${interaction.description}"
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(PactVerificationStateChangeExtension(pact, interaction, testResultAccumulator), this)
  }

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext
    return when (parameterContext.parameter.type) {
      Pact::class.java -> true
      Interaction::class.java -> true
      HttpRequest::class.java -> testContext.target is HttpTestTarget || testContext.target is HttpsTestTarget
      PactVerificationContext::class.java -> true
      ProviderVerifier::class.java -> true
      else -> false
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    return when (parameterContext.parameter.type) {
      Pact::class.java -> pact
      Interaction::class.java -> interaction
      HttpRequest::class.java -> store.get("httpRequest")
      PactVerificationContext::class.java -> store.get("interactionContext")
      ProviderVerifier::class.java -> store.get("verifier")
      else -> null
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    store.put("interactionContext", PactVerificationContext(store, context, consumerName = pact.consumer.name,
      interaction = interaction))
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    val providerInfo = testContext.target.getProviderInfo(serviceName, pactSource)
    testContext.providerInfo = providerInfo

    prepareVerifier(testContext, context)
    store.put("verifier", testContext.verifier)

    val requestAndClient = testContext.target.prepareRequest(interaction, testContext.executionContext ?: emptyMap())
    if (requestAndClient != null) {
      val (request, client) = requestAndClient
      store.put("request", request)
      store.put("client", client)
      if (testContext.target.isHttpTarget()) {
        store.put("httpRequest", request)
      }
    }
  }

  private fun prepareVerifier(testContext: PactVerificationContext, extContext: ExtensionContext) {
    val consumer = ConsumerInfo(consumerName ?: pact.consumer.name)

    val verifier = ProviderVerifier()
    testContext.target.prepareVerifier(verifier, extContext.requiredTestInstance)

    setupReporters(verifier, serviceName, interaction.description, extContext, testContext.valueResolver)

    verifier.initialiseReporters(testContext.providerInfo)
    verifier.reportVerificationForConsumer(consumer, testContext.providerInfo)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name, testContext.providerInfo, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    testContext.verifier = verifier
  }

  private fun setupReporters(
    verifier: IProviderVerifier,
    name: String,
    description: String,
    extContext: ExtensionContext,
    valueResolver: ValueResolver
  ) {
    var reportDirectory = "target/pact/reports"
    val reports = mutableListOf<String>()
    var reportingEnabled = false

    val verificationReports = AnnotationSupport.findAnnotation(extContext.requiredTestClass, VerificationReports::class.java)
    if (verificationReports.isPresent) {
      reportingEnabled = true
      reportDirectory = verificationReports.get().reportDir
      reports.addAll(verificationReports.get().value)
    } else if (valueResolver.propertyDefined("pact.verification.reports")) {
      reportingEnabled = true
      reportDirectory = valueResolver.resolveValue("pact.verification.reportDir:$reportDirectory")!!
      reports.addAll(valueResolver.resolveValue("pact.verification.reports:")!!.split(","))
    }

    if (reportingEnabled) {
      val reportDir = File(reportDirectory)
      reportDir.mkdirs()
      verifier.reporters = reports
        .filter { r -> r.isNotEmpty() }
        .map { r ->
          val reporter = ReporterManager.createReporter(r.trim())
          reporter.setReportDir(reportDir)
          reporter.setReportFile(File(reportDir, "$name - $description${reporter.ext}"))
          reporter
        }
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext
    testResultAccumulator.updateTestResult(pact, interaction, testContext.testExecutionResult)
  }

  companion object : KLogging()
}

/**
 * JUnit 5 test extension class for executing state change callbacks
 */
class PactVerificationStateChangeExtension(
  private val pact: Pact<Interaction>,
  private val interaction: Interaction,
  private val testResultAccumulator: TestResultAccumulator
) : BeforeTestExecutionCallback, AfterTestExecutionCallback {
  override fun beforeTestExecution(extensionContext: ExtensionContext) {
    logger.debug { "beforeEach for interaction '${interaction.description}'" }
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    try {
      val providerStateContext = invokeStateChangeMethods(extensionContext, testContext,
        interaction.providerStates, StateChangeAction.SETUP)
      testContext.executionContext = mapOf("providerState" to providerStateContext)
    } catch (e: Exception) {
      logger.error(e) { "Provider state change callback failed" }
      testResultAccumulator.updateTestResult(pact, interaction, TestResult.Failed(description = "Provider state change callback failed",
        results = listOf(mapOf("exception" to e))))
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    logger.debug { "afterEach for interaction '${interaction.description}'" }
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    invokeStateChangeMethods(context, testContext, interaction.providerStates, StateChangeAction.TEARDOWN)
  }

  private fun invokeStateChangeMethods(
    context: ExtensionContext,
    testContext: PactVerificationContext,
    providerStates: List<ProviderState>,
    action: StateChangeAction
  ): Map<String, Any?> {
    val errors = mutableListOf<String>()

    val providerStateContext = mutableMapOf<String, Any?>()
    providerStates.forEach { state ->
      val stateChangeMethods = findStateChangeMethods(context.requiredTestInstance,
        testContext.stateChangeHandlers, state)
      if (stateChangeMethods.isEmpty()) {
        errors.add("Did not find a test class method annotated with @State(\"${state.name}\")")
      } else {
        stateChangeMethods.filter { it.second.action == action }.forEach { (method, _, instance) ->
          logger.debug { "Invoking state change method ${method.name} for state '${state.name}' on $instance" }
          val stateChangeValue = if (method.parameterCount > 0) {
            ReflectionSupport.invokeMethod(method, instance, state.params)
          } else {
            ReflectionSupport.invokeMethod(method, instance)
          }

          if (stateChangeValue is Map<*, *>) {
            providerStateContext.putAll(stateChangeValue as Map<String, Any?>)
          }
        }
      }
    }

    if (errors.isNotEmpty()) {
      throw MissingStateChangeMethod(errors.joinToString("\n"))
    }

    return providerStateContext
  }

  private fun findStateChangeMethods(
    testClass: Any,
    stateChangeHandlers: List<Any>,
    state: ProviderState
  ): List<Triple<Method, State, Any>> {
    val stateChangeClasses =
      AnnotationSupport.findAnnotatedMethods(testClass.javaClass, State::class.java, HierarchyTraversalMode.TOP_DOWN)
        .map { it to testClass }
        .plus(stateChangeHandlers.flatMap { handler ->
          AnnotationSupport.findAnnotatedMethods(handler.javaClass, State::class.java, HierarchyTraversalMode.TOP_DOWN)
            .map { it to handler }
        })
    return stateChangeClasses
      .map { Triple(it.first, it.first.getAnnotation(State::class.java), it.second) }
      .filter { it.second.value.any { s -> state.name == s } }
  }

  companion object : KLogging()
}

/**
 * Main TestTemplateInvocationContextProvider for JUnit 5 Pact verification tests. This class needs to be applied to
 * a test template method on a test class annotated with a @Provider annotation.
 */
class PactVerificationInvocationContextProvider : TestTemplateInvocationContextProvider {
  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    logger.debug { "provideTestTemplateInvocationContexts called" }

    val providerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Provider::class.java)
    if (!providerInfo.isPresent) {
      throw UnsupportedOperationException("Provider name should be specified by using @${Provider::class.java.name} annotation")
    }
    val serviceName = providerInfo.get().value

    val consumerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Consumer::class.java)
    val consumerName = consumerInfo.orElse(null)?.value

    validateStateChangeMethods(context.requiredTestClass)

    logger.debug { "Verifying pacts for provider '$serviceName' and consumer '$consumerName'" }

    val pactSources = findPactSources(context).flatMap {
      filterPactsByAnnotations(it.load(serviceName), context.requiredTestClass).map { pact -> pact to it.pactSource }
    }.filter { p -> consumerName == null || p.first.consumer.name == consumerName }

    val tests = pactSources.flatMap { pact ->
      pact.first.interactions.map { PactVerificationExtension(pact.first, pact.second, it, serviceName, consumerName) }
    }
    return tests.stream() as Stream<TestTemplateInvocationContext>
  }

  private fun validateStateChangeMethods(testClass: Class<*>) {
    val errors = mutableListOf<String>()
    AnnotationSupport.findAnnotatedMethods(testClass, State::class.java, HierarchyTraversalMode.TOP_DOWN).forEach {
      if (it.parameterCount > 1) {
        errors.add("State change method ${it.name} should either take no parameters or a single Map parameter")
      } else if (it.parameterCount == 1 && !Map::class.java.isAssignableFrom(it.parameterTypes[0])) {
        errors.add("State change method ${it.name} should take only a single Map parameter")
      }
    }

    if (errors.isNotEmpty()) {
      throw UnsupportedOperationException(errors.joinToString("\n"))
    }
  }

  private fun findPactSources(context: ExtensionContext): List<PactLoader> {
    val pactSource = context.requiredTestClass.getAnnotation(PactSource::class.java)
    logger.debug { "Pact source on test class: $pactSource" }
    val pactLoaders = context.requiredTestClass.annotations.filter { annotation ->
      annotation.annotationClass.findAnnotation<PactSource>() != null
    }
    logger.debug { "Pact loaders on test class: $pactLoaders" }

    if (pactSource == null && pactLoaders.isEmpty()) {
      throw UnsupportedOperationException("At least one pact source must be present on the test class")
    }

    return pactLoaders.plus(pactSource).filterNotNull().map {
      if (it is PactSource) {
        val pactLoaderClass = pactSource.value
        try {
          // Checks if there is a constructor with one argument of type Class.
          val constructorWithClass = pactLoaderClass.java.getDeclaredConstructor(Class::class.java)
          if (constructorWithClass != null) {
            constructorWithClass.isAccessible = true
            constructorWithClass.newInstance(context.requiredTestClass)
          } else {
            pactLoaderClass.createInstance()
          }
        } catch (e: NoSuchMethodException) {
          logger.error(e) { e.message }
          pactLoaderClass.createInstance()
        }
      } else {
        it.annotationClass.findAnnotation<PactSource>()!!.value.java
          .getConstructor(it.annotationClass.java).newInstance(it)
      }
    }
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return AnnotationSupport.isAnnotated(context.requiredTestClass, Provider::class.java)
  }

  companion object : KLogging()
}
