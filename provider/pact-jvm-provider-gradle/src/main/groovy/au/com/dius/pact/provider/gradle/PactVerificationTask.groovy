package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderVerifier
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction

/**
 * Task to verify a pact against a provider
 */
class PactVerificationTask extends PactVerificationBaseTask {

  GradleProviderInfo providerToVerify

  @TaskAction
  void verifyPact() {
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
      providerVersion = providerToVerify.providerVersion ?: { project.version }
      if (providerToVerify.providerTag) {
        providerTag = providerToVerify.providerTag
      }

      if (project.pact.reports) {
        def reportsDir = new File(project.buildDir, 'reports/pact')
        reporters = project.pact.reports.toVerifierReporters(reportsDir, it)
      }
    }

    runVerification(verifier, providerToVerify)
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
