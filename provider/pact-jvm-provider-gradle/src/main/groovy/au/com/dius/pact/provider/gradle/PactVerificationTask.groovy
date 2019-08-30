package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderVerifier
import org.fusesource.jansi.AnsiConsole
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction

import java.util.function.Supplier

/**
 * Task to verify a pact against a provider
 */
class PactVerificationTask extends DefaultTask {

  ProviderInfo providerToVerify

  @TaskAction
  void verifyPact() {
    AnsiConsole.systemInstall()
    ProviderVerifier verifier = new ProviderVerifier()
    verifier.with {
      projectHasProperty = { project.hasProperty(it) }
      projectGetProperty = { project.property(it) }
      pactLoadFailureMessage = { 'You must specify the pactfile to execute (use pactFile = ...)' }
      checkBuildSpecificTask = { it instanceof Task || it instanceof String && project.tasks.findByName(it) }
      executeBuildSpecificTask = this.&executeStateChangeTask
      projectClasspath = {
        project.sourceSets.test.runtimeClasspath*.toURL()
      }

      providerVersion = (providerToVerify.version != null) ? providerToVerify.version as Supplier<String> : project.version as Supplier<String>

      if (project.pact.reports) {
        def reportsDir = new File(project.buildDir, 'reports/pact')
        reporters = project.pact.reports.toVerifierReporters(reportsDir)
      }
    }

    ext.failures = verifier.verifyProvider(providerToVerify)
    try {
      if (ext.failures.size() > 0) {
        verifier.displayFailures(ext.failures)
        throw new GradleScriptException(
          "There were ${ext.failures.size()} pact failures for provider ${providerToVerify.name}", null)
      }
    } finally {
      verifier.finaliseReports()
      AnsiConsole.systemUninstall()
    }
  }

  def executeStateChangeTask(t, state) {
    def task = t instanceof String ? project.tasks.getByName(t) : t
    task.setProperty('providerState', state)
    task.ext.providerState = state
    def build = project.task(type: GradleBuild) {
      tasks = [task.name]
    }
    build.execute()
  }
}
