package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.ProviderInfo
import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Main plugin class
 */
class PactPlugin implements Plugin<Project> {

    private static final GROUP = 'Pact'

    @Override
    void apply(Project project) {

        // Create and install the extension object
        project.extensions.create('pact', PactPluginExtension, project.container(GradleProviderInfo))

        project.task('pactVerify', description: 'Verify your pacts against your providers', group: GROUP)
        project.task('pactPublish', description: 'Publish your pacts to a pact broker', type: PactPublishTask,
            group: GROUP)

        project.afterEvaluate {

            if (project.pact == null) {
              throw new GradleScriptException('No pact block was found in the project', null)
            } else if (!(project.pact instanceof PactPluginExtension)) {
              throw new GradleScriptException('Your project is misconfigured, was expecting a \'pact\' configuration ' +
                "in the build, but got a ${project.pact.class.simpleName} with value '${project.pact}' instead. " +
                'Make sure there is no property that is overriding \'pact\'.', null)
            } else if (project.pact.serviceProviders.empty) {
              throw new GradleScriptException('No service providers are configured', null)
            }

            project.pact.serviceProviders.all { ProviderInfo provider ->
                def providerTask = project.task("pactVerify_${provider.name}",
                    description: "Verify the pacts against ${provider.name}", type: PactVerificationTask,
                    group: GROUP, dependsOn: 'testClasses') {
                    providerToVerify = provider
                }

                if (provider.startProviderTask != null) {
                    providerTask.dependsOn(provider.startProviderTask)
                }

                if (provider.terminateProviderTask != null) {
                    providerTask.finalizedBy(provider.terminateProviderTask)
                }

                project.pactVerify.dependsOn(providerTask)
            }
        }
    }
}
