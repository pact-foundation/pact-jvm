package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.IProviderVerifier
import au.com.dius.pact.provider.ProviderVerifier
import javax.inject.Inject
import org.gradle.api.GradleScriptException
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Task to verify a pact against a provider
 */
abstract class PactVerificationTask extends PactVerificationBaseTask {
  @Internal
  IProviderVerifier verifier = new ProviderVerifier()
  @Internal
  GradleProviderInfo providerToVerify

  @Inject
  protected abstract ProviderFactory getProviderFactory();

  @Input
  @Optional
  abstract ListProperty<URI> getTestClasspathURL()

  @Input
  abstract SetProperty<Task> getTaskContainer()

  @Input
  abstract Property<Object> getProjectVersion()

  @Input
  @Optional
  abstract Property<VerificationReports> getReport()

  @Input
  abstract Property<File> getBuildDir()

  @TaskAction
  void verifyPact() {
    verifier.with {
      verificationSource = 'gradle'
      projectHasProperty = { providerFactory.gradleProperty(it).present }
      projectGetProperty = { providerFactory.gradleProperty(it).get() }
      pactLoadFailureMessage = { 'You must specify the pact file to execute (use pactSource = file(...) etc.)' }
      checkBuildSpecificTask = {
        it instanceof Task || it instanceof String && taskContainer.get().find { t -> t.name == it }
      }
      executeBuildSpecificTask = this.&executeStateChangeTask
      projectClasspath = { testClasspathURL.get() }
      providerVersion = providerToVerify.providerVersion ?: { projectVersion.get() }
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
      }

      def report = report.getOrElse(null)
      if (report != null) {
        def reportsDir = new File(buildDir.get(), 'reports/pact')
        reporters = report.toVerifierReporters(reportsDir, it)
      }
    }

    if (providerToVerify.consumers.empty && !ignoreNoConsumers()) {
      throw new GradleScriptException("There are no consumers for service provider '${providerToVerify.name}'", null)
    }

    runVerification(verifier, providerToVerify)
  }

  def executeStateChangeTask(t, state) {
    def taskSet = taskContainer.get()
    def task = t instanceof String ? taskSet.find { it.name == t } : t
    task.setProperty('providerState', state)
    task.ext.providerState = state
    def build = project.task(type: GradleBuild) {
      tasks = [task.name]
    }
    build.execute()
  }
}
