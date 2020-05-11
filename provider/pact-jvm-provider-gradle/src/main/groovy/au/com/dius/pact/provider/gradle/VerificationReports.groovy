package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.reporters.ReporterManager
import groovy.transform.ToString
import org.gradle.api.GradleScriptException

/**
 * Reports configuration object
 */
@ToString
class VerificationReports {
  Map reports = [:]

  def defaultReports() {
    reports.console = ReporterManager.createReporter('console')
  }

  List toVerifierReporters(File reportDir, IProviderVerifier verifier) {
    reports.values().collect {
      it.reportDir = reportDir
      it.verifier = verifier
      it
    }
  }

  def propertyMissing(String name) {
    if (ReporterManager.reporterDefined(name)) {
      reports[name] = ReporterManager.createReporter(name)
    } else {
      throw new GradleScriptException("There is no defined reporter named '$name'. Available reporters are: " +
        "${ReporterManager.availableReporters()}", null)
    }
  }

}
