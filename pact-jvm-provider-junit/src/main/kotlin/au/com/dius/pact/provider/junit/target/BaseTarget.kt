package au.com.dius.pact.provider.junit.target

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.PactSource
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junit.JUnitProviderTestSupport
import au.com.dius.pact.provider.junit.VerificationReports
import au.com.dius.pact.provider.reporters.ReporterManager
import au.com.dius.pact.support.expressions.SystemPropertyResolver
import au.com.dius.pact.support.expressions.ValueResolver
import org.junit.runners.model.TestClass
import java.io.File
import java.util.function.BiConsumer

/**
 * Out-of-the-box implementation of [Target],
 * that run [Interaction] against message pact and verify response
 */
abstract class BaseTarget : TestClassAwareTarget {

  protected lateinit var testClass: TestClass
  protected lateinit var testTarget: Any

  var valueResolver: ValueResolver = SystemPropertyResolver()
  private val callbacks = mutableListOf<BiConsumer<Boolean, ProviderVerifier>>()

  protected abstract fun getProviderInfo(source: PactSource): ProviderInfo

  protected abstract fun setupVerifier(
    interaction: Interaction,
    provider: ProviderInfo,
    consumer: ConsumerInfo
  ): ProviderVerifier

  protected fun setupReporters(verifier: ProviderVerifier, name: String, description: String) {
    var reportDirectory = "target/pact/reports"
    var reports = arrayOf<String>()
    var reportingEnabled = false

    val verificationReports = testClass.getAnnotation(VerificationReports::class.java)
    if (verificationReports != null) {
      reportingEnabled = true
      reportDirectory = verificationReports.reportDir
      reports = verificationReports.value
    } else if (valueResolver.propertyDefined("pact.verification.reports")) {
      reportingEnabled = true
      reportDirectory = valueResolver.resolveValue("pact.verification.reportDir:$reportDirectory")
      reports = valueResolver.resolveValue("pact.verification.reports:").split(",").toTypedArray()
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

  protected fun getAssertionError(mismatches: Map<String, Any>): AssertionError {
    return AssertionError(JUnitProviderTestSupport.generateErrorStringFromMismatches(mismatches))
  }

  override fun setTestClass(testClass: TestClass, testTarget: Any) {
    this.testClass = testClass
    this.testTarget = testTarget
  }

  override fun addResultCallback(callback: BiConsumer<Boolean, ProviderVerifier>) {
    this.callbacks.add(callback)
  }

  protected fun reportTestResult(result: Boolean, verifier: ProviderVerifier) {
    this.callbacks.forEach { callback -> callback.accept(result, verifier) }
  }
}
