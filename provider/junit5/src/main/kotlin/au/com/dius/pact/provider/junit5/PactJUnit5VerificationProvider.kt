package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.handleWith
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.StateChangeResult
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.checkForOverriddenPactUrl
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport.filterPactsByAnnotations
import au.com.dius.pact.provider.junitsupport.MissingStateChangeMethod
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.StateChangeAction
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.loader.NoPactsFoundException
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import au.com.dius.pact.provider.junitsupport.loader.PactSource
import au.com.dius.pact.provider.reporters.ReporterManager
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrElse
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

val namespace: ExtensionContext.Namespace = ExtensionContext.Namespace.create("pact-jvm")

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
  var providerInfo: IProviderInfo,
  val consumer: IConsumerInfo,
  val interaction: Interaction,
  var testExecutionResult: MutableList<VerificationResult.Failed> = mutableListOf()
) {
  val stateChangeHandlers: MutableList<Any> = mutableListOf()
  var executionContext: Map<String, Any>? = null

  /**
   * Called to verify the interaction from the test template method.
   *
   * @throws AssertionError Throws an assertion error if the verification fails.
   */
  fun verifyInteraction() {
    val store = context.getStore(namespace)
    val client = store.get("client")
    val request = store.get("request")
    val testContext = store.get("interactionContext") as PactVerificationContext
    try {
      val result = validateTestExecution(client, request, testContext.executionContext ?: emptyMap())
        .filterIsInstance<VerificationResult.Failed>()
      this.testExecutionResult.addAll(result)
      if (testExecutionResult.isNotEmpty()) {
        verifier!!.displayFailures(testExecutionResult)
        if (testExecutionResult.any { !it.pending }) {
          throw AssertionError(verifier!!.generateErrorStringFromVerificationResult(testExecutionResult))
        }
      }
    } finally {
      verifier!!.finaliseReports()
    }
  }

  private fun validateTestExecution(
    client: Any?,
    request: Any?,
    context: Map<String, Any>
  ): List<VerificationResult> {
    if (providerInfo.verificationType == null || providerInfo.verificationType == PactVerification.REQUEST_RESPONSE) {
      val interactionMessage = "Verifying a pact between ${consumer.name} and ${providerInfo.name}" +
        " - ${interaction.description}"
      return try {
        val reqResInteraction = interaction as RequestResponseInteraction
        val expectedResponse = reqResInteraction.response.generatedResponse(context)
        val actualResponse = target.executeInteraction(client, request)

        listOf(verifier!!.verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, mutableMapOf(),
          reqResInteraction.interactionId.orEmpty(), consumer.pending))
      } catch (e: Exception) {
        verifier!!.reporters.forEach {
          it.requestFailed(providerInfo, interaction, interactionMessage, e,
            verifier!!.projectHasProperty.apply(ProviderVerifier.PACT_SHOW_STACKTRACE))
        }
        listOf(VerificationResult.Failed(listOf(mapOf("message" to "Request to provider failed with an exception",
          "exception" to e)),
          "Request to provider failed with an exception", interactionMessage,
          listOf(VerificationFailureType.ExceptionFailure("Request to provider failed with an exception", e)),
          consumer.pending, interaction.interactionId))
      }
    } else {
      return listOf(verifier!!.verifyResponseByInvokingProviderMethods(providerInfo, consumer, interaction,
        interaction.description, mutableMapOf()))
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
  private val pact: Pact,
  private val pactSource: au.com.dius.pact.core.model.PactSource,
  private val interaction: Interaction,
  private val serviceName: String,
  private val consumerName: String?
) : TestTemplateInvocationContext, ParameterResolver, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback {

  private val testResultAccumulator = DefaultTestResultAccumulator

  override fun getDisplayName(invocationIndex: Int): String {
    return when {
      pactSource is BrokerUrlSource && pactSource.result != null -> {
        var displayName = pactSource.result!!.name + " - ${interaction.description}"
        if (pactSource.tag.isNotEmpty()) displayName += " (tag ${pactSource.tag})"
        if (pactSource.result!!.pending) {
          "$displayName [PENDING]"
        } else {
          displayName
        }
      }
      pactSource is BrokerUrlSource && pactSource.tag.isNotEmpty() -> "${pact.consumer.name} - ${interaction.description} (tag ${pactSource.tag})"
      else -> "${pact.consumer.name} - ${interaction.description}"
    }
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(PactVerificationStateChangeExtension(interaction, pactSource), this)
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
    val store = context.getStore(namespace)
    val pending = pactSource is BrokerUrlSource && pactSource.result?.pending == true
    val verificationContext = PactVerificationContext(store, context,
      consumer = ConsumerInfo(pact.consumer.name, pactSource = pactSource, pending = pending),
      interaction = interaction, providerInfo = ProviderInfo(serviceName))
    store.put("interactionContext", verificationContext)
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    val store = context.getStore(namespace)
    val testContext = store.get("interactionContext") as PactVerificationContext

    val providerInfo = testContext.target.getProviderInfo(serviceName, pactSource)
    testContext.providerInfo = providerInfo

    prepareVerifier(testContext, context, pactSource)
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

  private fun prepareVerifier(testContext: PactVerificationContext, extContext: ExtensionContext, pactSource: au.com.dius.pact.core.model.PactSource) {
    val consumer = when {
      pactSource is BrokerUrlSource && pactSource.result != null -> ConsumerInfo(pactSource.result!!.name,
        pactSource = pactSource, notices = pactSource.result!!.notices, pending = pactSource.result!!.pending)
      else -> ConsumerInfo(consumerName ?: pact.consumer.name)
    }

    val verifier = ProviderVerifier()
    testContext.target.prepareVerifier(verifier, extContext.requiredTestInstance)

    setupReporters(verifier, serviceName, interaction.description, extContext, testContext.valueResolver)

    verifier.initialiseReporters(testContext.providerInfo)
    verifier.reportVerificationForConsumer(consumer, testContext.providerInfo, pactSource)

    if (interaction.providerStates.isNotEmpty()) {
      for ((name) in interaction.providerStates) {
        verifier.reportStateForInteraction(name.toString(), testContext.providerInfo, consumer, true)
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
          val reporter = ReporterManager.createReporter(r.trim(), reportDir, verifier)
          reporter.reportFile = File(reportDir, "$name - $description${reporter.ext}")
          reporter
        }
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext
    val pact = if (this.pact is FilteredPact) pact.pact else pact
    testResultAccumulator.updateTestResult(pact, interaction, testContext.testExecutionResult, pactSource)
  }

  companion object : KLogging()
}

/**
 * JUnit 5 test extension class for executing state change callbacks
 */
class PactVerificationStateChangeExtension(
  private val interaction: Interaction,
  private val pactSource: au.com.dius.pact.core.model.PactSource
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
      val pending = pactSource is BrokerUrlSource && pactSource.result?.pending == true
      logger.error(e) { "Provider state change callback failed" }
      testContext.testExecutionResult.add(VerificationResult.Failed(description = "Provider state change callback failed",
        results = listOf(mapOf("exception" to e)),
        failures = listOf(VerificationFailureType.StateChangeFailure("Provider state change callback failed", StateChangeResult(Err(e)))),
        pending = pending,
        interactionId = interaction.interactionId
      ))
      if (!pending) {
        throw AssertionError("Provider state change callback failed", e)
      }
    }
  }

  override fun afterTestExecution(context: ExtensionContext) {
    logger.debug { "afterEach for interaction '${interaction.description}'" }
    val store = context.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext

    try {
      invokeStateChangeMethods(context, testContext, interaction.providerStates, StateChangeAction.TEARDOWN)
    } catch (e: Exception) {
      val pending = pactSource is BrokerUrlSource && pactSource.result?.pending == true
      logger.error(e) { "Provider state change callback failed" }
      testContext.testExecutionResult.add(VerificationResult.Failed(description = "Provider state change teardown callback failed",
        results = listOf(mapOf("exception" to e)),
        failures = listOf(VerificationFailureType.StateChangeFailure("Provider state change teardown callback failed", StateChangeResult(Err(e)))),
        pending = pending,
        interactionId = interaction.interactionId
      ))
      if (!pending) {
        throw AssertionError("Provider state change callback failed", e)
      }
    }
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
        errors.add("Did not find a test class method annotated with @State(\"${state.name}\") \n" +
          "for Interaction \"${testContext.interaction.description}\" \n" +
          "with Consumer \"${testContext.consumer.name}\"")
      } else {
        stateChangeMethods.filter { it.second.action == action }.forEach { (method, stateAnnotation, instance) ->
          logger.info {
            val name = stateAnnotation.value.joinToString(", ")
            if (stateAnnotation.comment.isNotEmpty()) {
              "Invoking state change method '$name':${stateAnnotation.action} (${stateAnnotation.comment})"
            } else {
              "Invoking state change method '$name':${stateAnnotation.action}"
            }
          }
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
open class PactVerificationInvocationContextProvider : TestTemplateInvocationContextProvider {

  override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
    logger.debug { "provideTestTemplateInvocationContexts called" }
    val tests = resolvePactSources(context)
    return when {
      tests.first.isNotEmpty() -> tests.first.stream() as Stream<TestTemplateInvocationContext>
      AnnotationSupport.isAnnotated(context.requiredTestClass, IgnoreNoPactsToVerify::class.java) ->
        listOf(DummyTestTemplate).stream() as Stream<TestTemplateInvocationContext>
      else -> throw NoPactsFoundException("No Pact files were found to verify\n${tests.second}")
    }
  }

  private fun resolvePactSources(context: ExtensionContext): Pair<List<PactVerificationExtension>, String> {
    var description = ""
    val providerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Provider::class.java)
    val serviceName = if (providerInfo.isPresent && providerInfo.get().value.isNotEmpty()) {
      providerInfo.get().value
    } else {
      System.getProperty("pact.provider.name")
    }
    if (serviceName.isNullOrEmpty()) {
      throw UnsupportedOperationException("Provider name should be specified by using either " +
        "@${Provider::class.java.name} annotation or the 'pact.provider.name' system property")
    }
    description += "Provider: $serviceName"

    val consumerInfo = AnnotationSupport.findAnnotation(context.requiredTestClass, Consumer::class.java)
    val consumerName = consumerInfo.orElse(null)?.value
    if (consumerName.isNotEmpty()) {
      description += "\nConsumer: $consumerName"
    }

    validateStateChangeMethods(context.requiredTestClass)

    logger.debug { "Verifying pacts for provider '$serviceName' and consumer '$consumerName'" }

    val pactSources = findPactSources(context).flatMap { loader ->
      val valueResolver = getValueResolver(context)
      if (valueResolver != null) {
        loader.setValueResolver(valueResolver)
      }
      description += "\nSource: ${loader.description()}"
      val pacts = handleWith<List<Pact>> { loader.load(serviceName) }.getOrElse {
        val ignoreAnnotation = AnnotationSupport.findAnnotation(context.requiredTestClass, IgnoreNoPactsToVerify::class.java)
        if (ignoreAnnotation.isPresent && ignoreAnnotation.get().ignoreIoErrors == "true") {
          emptyList()
        } else {
          throw it
        }
      }
      filterPactsByAnnotations(pacts, context.requiredTestClass)
    }.filter { p -> consumerName == null || p.consumer.name == consumerName }

    val interactionFilter = System.getProperty("pact.filter.description")
    return Pair(pactSources.flatMap { pact ->
      pact.interactions
        .filter {
          interactionFilter.isNullOrEmpty() || it.description.matches(Regex(interactionFilter))
        }
        .map { PactVerificationExtension(pact, pact.source, it, serviceName, consumerName) }
    }, description)
  }

  protected open fun getValueResolver(context: ExtensionContext): ValueResolver? = null

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
          constructorWithClass.isAccessible = true
          constructorWithClass.newInstance(context.requiredTestClass)
        } catch (e: NoSuchMethodException) {
          logger.error(e) { e.message }
          pactLoaderClass.createInstance()
        }
      } else {
        it.annotationClass.findAnnotation<PactSource>()!!.value.java
          .getConstructor(it.annotationClass.java).newInstance(it)
      }
    }.map {
      checkForOverriddenPactUrl(it,
        context.requiredTestClass.getAnnotation(AllowOverridePactUrl::class.java),
        context.requiredTestClass.getAnnotation(Consumer::class.java))
      it
    }
  }

  override fun supportsTestTemplate(context: ExtensionContext): Boolean {
    return AnnotationSupport.isAnnotated(context.requiredTestClass, Provider::class.java)
  }

  companion object : KLogging()
}
