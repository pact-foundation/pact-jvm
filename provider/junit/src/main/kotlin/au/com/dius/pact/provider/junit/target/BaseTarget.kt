package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IConsumerInfo
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.VerificationResult
import au.com.dius.pact.provider.junitsupport.VerificationReports
import au.com.dius.pact.provider.junitsupport.target.Target
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.ReporterManager
import org.apache.commons.lang3.tuple.Pair
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.io.File
import java.util.function.BiConsumer
import java.util.function.Supplier
import au.com.dius.pact.core.support.BuiltToolConfig.detectedBuildToolPactDirectory
import au.com.dius.pact.provider.ProviderUtils
import org.apache.commons.io.FilenameUtils

/**
 * Out-of-the-box implementation of [Target],
 * that run [Interaction] against message pact and verify response
 */
abstract class BaseTarget : TestClassAwareTarget {

  protected lateinit var testClass: TestClass
  protected lateinit var testTarget: Any

  var valueResolver: ValueResolver = SystemPropertyResolver()
  private val callbacks = mutableListOf<BiConsumer<VerificationResult, IProviderVerifier>>()
  private val stateHandlers = mutableListOf<Pair<Class<out Any>, Supplier<out Any>>>()

  protected lateinit var provider: IProviderInfo
  protected lateinit var consumer: IConsumerInfo
  override lateinit var verifier: IProviderVerifier

  protected abstract fun getProviderInfo(source: PactSource): ProviderInfo

  protected abstract fun setupVerifier(
    interaction: Interaction,
    provider: IProviderInfo,
    consumer: IConsumerInfo,
    pactSource: PactSource?
  ): IProviderVerifier

  protected fun setupReporters(verifier: IProviderVerifier) {
    var reportDirectory = FilenameUtils.concat(detectedBuildToolPactDirectory(), "reports")
    var reportingEnabled = false

    val verificationReports = ProviderUtils.findAnnotation(testClass.javaClass, VerificationReports::class.java)
      val reports: List<String> = when {
      verificationReports != null -> {
        reportingEnabled = true
        if (verificationReports.reportDir.isNotEmpty()) {
          reportDirectory = verificationReports.reportDir
        }
        verificationReports.value.toList()
      }
      valueResolver.propertyDefined("pact.verification.reports") -> {
        reportingEnabled = true
        val directory = valueResolver.resolveValue("pact.verification.reportDir:$reportDirectory")!!
        if (directory.isNotEmpty()) {
          reportDirectory = directory
        }
        valueResolver.resolveValue("pact.verification.reports:")!!.split(",")
      }
      else -> emptyList()
    }

    if (reportingEnabled) {
      val reportDir = File(reportDirectory)
      reportDir.mkdirs()
      val reporters = reports
        .filter { r -> r.isNotEmpty() }
        .map { r ->
          val reporter = ReporterManager.createReporter(r.trim(), reportDir, verifier)
          reporter
        }
      if (reporters.none { it is AnsiConsoleReporter }) {
        verifier.reporters = listOf(ReporterManager.createReporter("console", null, verifier)) + reporters
      } else {
        verifier.reporters = reporters
      }
    } else {
      verifier.reporters = listOf(ReporterManager.createReporter("console", null, verifier))
    }
  }

  override fun setTestClass(testClass: TestClass, testTarget: Any) {
    this.testClass = testClass
    this.testTarget = testTarget
  }

  override fun addResultCallback(callback: BiConsumer<VerificationResult, IProviderVerifier>) {
    this.callbacks.add(callback)
  }

  protected fun reportTestResult(result: VerificationResult, verifier: IProviderVerifier) {
    this.callbacks.forEach { callback -> callback.accept(result, verifier) }
  }

  override fun setStateHandlers(stateHandlers: List<Pair<Class<out Any>, Supplier<out Any>>>) {
    this.stateHandlers.addAll(stateHandlers)
  }

  override fun getStateHandlers() = stateHandlers.toList()

  override fun withStateHandlers(vararg stateHandlers: Pair<Class<out Any>, Supplier<out Any>>): Target {
    setStateHandlers(stateHandlers.asList())
    return this
  }

  override fun withStateHandler(stateHandler: Pair<Class<out Any>, Supplier<out Any>>): Target {
    this.stateHandlers.add(stateHandler)
    return this
  }

  protected fun validateTargetRequestFilters(methods: MutableList<FrameworkMethod>) {
    methods.forEach { method ->
      val requestClass = getRequestClass()
      if (method.method.parameterTypes.size != 1) {
        throw Exception("Method ${method.name} should take only a single ${requestClass.simpleName} parameter")
      } else if (!requestClass.isAssignableFrom(method.method.parameterTypes[0])) {
        throw Exception("Method ${method.name} should take only a single ${requestClass.simpleName} parameter")
      }
    }
  }

  protected fun consumerInfo(consumerName: String, source: PactSource): IConsumerInfo {
    return when (source) {
      is BrokerUrlSource -> {
        val brokerResult = source.result
        if (brokerResult != null) {
          ConsumerInfo(consumerName, pactSource = source, notices = brokerResult.notices, pending = brokerResult.pending)
        } else {
          ConsumerInfo(consumerName, pactSource = source)
        }
      }
      else -> ConsumerInfo(consumerName, pactSource = source)
    }
  }

  override fun configureVerifier(source: PactSource, consumerName: String, interaction: Interaction) {
    provider = getProviderInfo(source)
    consumer = consumerInfo(consumerName, source)
    verifier = setupVerifier(interaction, provider, consumer, source)
  }
}
