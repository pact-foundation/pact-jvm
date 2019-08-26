package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderInfo
import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

/**
 * Main plugin class
 */
class PactPlugin implements Plugin<Project> {

  private static final GROUP = 'Pact'
  private static final String PACT_VERIFY = 'pactVerify'
  private static final String TEST_CLASSES = 'testClasses'

  @Override
    void apply(Project project) {

        // Create and install the extension object
        project.extensions.create('pact', PactPluginExtension, project.container(GradleProviderInfo))

        project.task(PACT_VERIFY, description: 'Verify your pacts against your providers', group: GROUP)
        project.task('pactPublish', description: 'Publish your pacts to a pact broker', type: PactPublishTask,
            group: GROUP)

        project.afterEvaluate {

            if (it.pact == null) {
              throw new GradleScriptException('No pact block was found in the project', null)
            } else if (!(it.pact instanceof PactPluginExtension)) {
              throw new GradleScriptException('Your project is misconfigured, was expecting a \'pact\' configuration ' +
                "in the build, but got a ${it.pact.class.simpleName} with value '${it.pact}' instead. " +
                'Make sure there is no property that is overriding \'pact\'.', null)
            } else if (it.pact.serviceProviders.empty
              && it.gradle.startParameter.taskNames.any { it.equalsIgnoreCase(PACT_VERIFY) }) {
              throw new GradleScriptException('No service providers are configured', null)
            }

            it.pact.serviceProviders.all { ProviderInfo provider ->
                def taskName = {
                  def defaultName = "pactVerify_${provider.name.replaceAll(/\s+/, '_')}".toString()
                  try {
                    def clazz = this.getClass().classLoader.loadClass('org.gradle.util.NameValidator').metaClass
                    def asValidName = clazz.getMetaMethod('asValidName', [String])
                    if (asValidName) {
                      return asValidName.invoke(clazz.newInstance(), [ defaultName ])
                    }
                    // Gradle versions > 4.6 no longer have an instance method
                    return defaultName
                  } catch (ClassNotFoundException e) {
                    // Earlier versions of Gradle don't have NameValidator
                    // Without it, we just don't change the task name
                    return defaultName
                  } catch (NoSuchMethodException e) {
                    // Gradle versions > 4.6 no longer have an instance method
                    return defaultName
                  }
                } ()

                def providerTask = project.task(taskName,
                    description: "Verify the pacts against ${provider.name}", type: PactVerificationTask,
                    group: GROUP) {
                    providerToVerify = provider
                }

                if (project.tasks.findByName(TEST_CLASSES)) {
                  providerTask.dependsOn TEST_CLASSES
                }

                if (provider.startProviderTask != null) {
                    providerTask.dependsOn(provider.startProviderTask)
                }

                if (provider.terminateProviderTask != null) {
                    providerTask.finalizedBy(provider.terminateProviderTask)
                }

                if (provider.dependencyForPactVerify) {
                    it.pactVerify.dependsOn(providerTask)
                }
            }
        }
    }
}
