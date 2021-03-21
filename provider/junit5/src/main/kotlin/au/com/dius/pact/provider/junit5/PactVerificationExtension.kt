package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.FilteredPact
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.DefaultTestResultAccumulator
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.TestResultAccumulator
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.reporters.ReporterManager
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
import org.junit.platform.commons.support.AnnotationSupport
import java.io.File

/**
 * JUnit 5 test extension class used to inject parameters and execute the test for a Pact interaction.
 */
class PactVerificationExtension(
  private val pact: Pact<Interaction>,
  private val pactSource: au.com.dius.pact.core.model.PactSource,
  private val interaction: Interaction,
  private val serviceName: String,
  private val consumerName: String?,
  private val propertyResolver: ValueResolver = SystemPropertyResolver
) : TestTemplateInvocationContext, ParameterResolver, BeforeEachCallback, BeforeTestExecutionCallback,
  AfterTestExecutionCallback {

  private val testResultAccumulator: TestResultAccumulator = DefaultTestResultAccumulator

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
    testResultAccumulator.updateTestResult(pact, interaction, testContext.testExecutionResult,
      pactSource, propertyResolver)
  }

  companion object : KLogging()
}
