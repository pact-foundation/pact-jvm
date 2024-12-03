package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.matchers.generators.ArrayContainsJsonGenerator
import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.RequestData
import au.com.dius.pact.provider.RequestDataToBeVerified
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.VerificationFailureType
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.reporters.ReporterManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.HttpRequest
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.platform.commons.support.AnnotationSupport
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * JUnit 5 test extension class used to inject parameters and execute the test for a Pact interaction.
 */
open class PactVerificationExtension(
  val pact: Pact,
  val pactSource: au.com.dius.pact.core.model.PactSource,
  val interaction: Interaction,
  val serviceName: String,
  val consumerName: String?,
  val propertyResolver: ValueResolver = SystemPropertyResolver
) : TestTemplateInvocationContext, ParameterResolver, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback {

  var testResultAccumulator: TestResultAccumulator = DefaultTestResultAccumulator

  override fun getDisplayName(invocationIndex: Int): String {
    val displayName = when {
      pactSource is BrokerUrlSource && pactSource.result != null -> {
        var displayName = pactSource.result!!.name + " - ${interaction.description}"
        if (pactSource.tag.isNotEmpty()) displayName += " (tag ${pactSource.tag})"
        displayName
      }
      pactSource is BrokerUrlSource && pactSource.tag.isNotEmpty() ->
        "${pact.consumer.name} - ${interaction.description} (tag ${pactSource.tag})"
      else -> "${pact.consumer.name} - ${interaction.description}"
    }
    return when {
      interaction.isV4() && interaction.asV4Interaction().pending -> "$displayName [PENDING]"
      pactSource is BrokerUrlSource && pactSource.result?.pending == true -> "$displayName [PENDING]"
      else -> displayName
    }
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return mutableListOf(PactVerificationStateChangeExtension(interaction, pactSource), this)
  }

  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    val testContext = store.get("interactionContext") as PactVerificationContext?
    return when (parameterContext.parameter.type) {
      Pact::class.java -> true
      Interaction::class.java -> true
      ClassicHttpRequest::class.java, HttpRequest::class.java -> testContext.hasMultipleTargets() || testContext?.currentTarget() is HttpTestTarget
      PactVerificationContext::class.java -> true
      ProviderVerifier::class.java -> true
      RequestData::class.java -> testContext.hasMultipleTargets() || testContext?.currentTarget() is PluginTestTarget
      else -> false
    }
  }

  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
    val store = extensionContext.getStore(ExtensionContext.Namespace.create("pact-jvm"))
    return when (parameterContext.parameter.type) {
      Pact::class.java -> pact
      Interaction::class.java -> interaction
      ClassicHttpRequest::class.java, HttpRequest::class.java -> store.get("httpRequest")
      PactVerificationContext::class.java -> store.get("interactionContext")
      ProviderVerifier::class.java -> store.get("verifier")
      RequestData::class.java -> {
        val request = store.get("request")
        if (request is RequestDataToBeVerified) {
          request
        } else {
          null
        }
      }
      else -> null
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    val store = context.getStore(namespace)
    val pending = interaction.isV4() && interaction.asV4Interaction().pending ||
      pactSource is BrokerUrlSource && pactSource.result?.pending == true
    val verificationContext = PactVerificationContext(
      store,
      context,
      consumer = ConsumerInfo(pact.consumer.name, pactSource = pactSource, pending = pending),
      interaction = interaction,
      pact = pact,
      providerInfo = ProviderInfo(serviceName),
      valueResolver = propertyResolver
    )
    store.put("interactionContext", verificationContext)
  }

  override fun beforeTestExecution(context: ExtensionContext) {
    val store = context.getStore(namespace)
    val testContext = store.get("interactionContext") as PactVerificationContext

    val target = testContext.currentTarget()
      ?: throw UnsupportedOperationException(
        "No test target has been configured for ${interaction.javaClass.simpleName} interactions")
    val providerInfo = target.getProviderInfo(serviceName, pactSource)
    testContext.providerInfo = providerInfo

    prepareVerifier(testContext, context, pactSource, target)
    store.put("verifier", testContext.verifier)

    val executionContext = testContext.executionContext ?: mutableMapOf()
    executionContext["ArrayContainsJsonGenerator"] = ArrayContainsJsonGenerator
    val requestAndClient = target.prepareRequest(pact, interaction, executionContext)
    if (requestAndClient != null) {
      val (request, client) = requestAndClient
      store.put("request", request)
      store.put("client", client)
      if (target.isHttpTarget()) {
        store.put("httpRequest", request)
      }
    }
  }

  private fun prepareVerifier(
    testContext: PactVerificationContext,
    extContext: ExtensionContext,
    pactSource: au.com.dius.pact.core.model.PactSource,
    target: TestTarget
  ) {
    val consumer = when {
      pactSource is BrokerUrlSource && pactSource.result != null -> ConsumerInfo(pactSource.result!!.name,
        pactSource = pactSource, notices = pactSource.result!!.notices, pending = pactSource.result!!.pending)
      else -> ConsumerInfo(consumerName ?: pact.consumer.name)
    }

    val verifier = ProviderVerifier()
    verifier.verificationSource = "junit5"
    target.prepareVerifier(verifier, extContext.requiredTestInstance, pact)

    setupReporters(verifier, serviceName, interaction.description, extContext, testContext.valueResolver)

    verifier.initialisePlugins(pact)
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
    if (context.executionException.isPresent) {
      val e = context.executionException.get()
      val failure = VerificationResult.Failed("Test method has failed with an exception: ${e.message}",
        failures = mapOf(
          interaction.interactionId.orEmpty() to
            listOf(VerificationFailureType.ExceptionFailure("Test method has failed with an exception",
              e, interaction))
        )
      )
      testResultAccumulator.updateTestResult(
        pact, interaction, testContext.testExecutionResult + failure,
        pactSource, propertyResolver
      )
    } else {
      val updateTestResult = testResultAccumulator.updateTestResult(
        pact, interaction, testContext.testExecutionResult,
        pactSource, propertyResolver
      )
      if (updateTestResult is Result.Err) {
        throw AssertionError("Failed to update the test results: " + updateTestResult.error.joinToString("\n"))
      }
    }
  }
}
