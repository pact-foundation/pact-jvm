package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.reporters.ReporterManager
import au.com.dius.pact.provider.reporters.VerifierReporter
import groovy.lang.GroovyObjectSupport
import org.gradle.api.GradleScriptException
import java.io.File

/**
 * Reports configuration object
 */
open class VerificationReports @JvmOverloads constructor(
  var reports: MutableMap<String, VerifierReporter> = mutableMapOf()
) : GroovyObjectSupport() {
  open fun defaultReports() {
    reports["console"] = ReporterManager.createReporter("console")
  }

  open fun toVerifierReporters(reportDir: File, verifier: IProviderVerifier): List<VerifierReporter> {
    return reports.values.map {
      it.reportDir = reportDir
      it.verifier = verifier
      it
    }
  }

  open fun propertyMissing(name: String): Any? {
    if (ReporterManager.reporterDefined(name)) {
      reports[name] = ReporterManager.createReporter(name)
      return reports[name]
    } else {
      throw GradleScriptException("There is no defined reporter named '$name'. Available reporters are: " +
        "${ReporterManager.availableReporters()}", null)
    }
  }
}
