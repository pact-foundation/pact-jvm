package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderVerifier
import org.gradle.api.GradleScriptException
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction

/**
 * Task to verify a pact against a provider
 */
class PactVerificationTask extends PactVerificationBaseTask {
  IProviderVerifier verifier = new ProviderVerifier()
  GradleProviderInfo providerToVerify

  @TaskAction
  void verifyPact() {
    verifier.with {
      verificationSource = 'gradle'
      projectHasProperty = { project.hasProperty(it) }
      projectGetProperty = { project.property(it) }
      pactLoadFailureMessage = { 'You must specify the pactfile to execute (use pactFile = ...)' }
      checkBuildSpecificTask = { it instanceof Task || it instanceof String && project.tasks.findByName(it) }
      executeBuildSpecificTask = this.&executeStateChangeTask
      projectClasspath = {
        project.sourceSets.test.runtimeClasspath*.toURL()
      }
      providerVersion = providerToVerify.providerVersion ?: { project.version }
      if (providerToVerify.providerTags) {
        if (providerToVerify.providerTags instanceof Closure ) {
          providerTags = providerToVerify.providerTags
        } else if (providerToVerify.providerTags instanceof List) {
          providerTags = { providerToVerify.providerTags }
        } else if (providerToVerify.providerTags instanceof String) {
          providerTags = { [ providerToVerify.providerTags ] }
        } else {
          throw new GradleScriptException(
            "${providerToVerify.providerTags} is not a valid value for providerTags", null)
        }
      } else if (providerToVerify.providerTag) {
        if (providerToVerify.providerTag instanceof Closure) {
          providerTags = { [ providerToVerify.providerTag.call() ] }
        } else {
          providerTags = { [ providerToVerify.providerTag ] }
        }
      }

      if (project.pact.reports) {
        def reportsDir = new File(project.buildDir, 'reports/pact')
        reporters = project.pact.reports.toVerifierReporters(reportsDir, it)
      }
    }

    if (providerToVerify.consumers.empty && !ignoreNoConsumers()) {
      throw new GradleScriptException("There are no consumers for service provider '${providerToVerify.name}'", null)
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
