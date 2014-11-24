package au.com.dius.pact.provider.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class PactPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        // Create and install the extension object
        project.extensions.create("pact", PactPluginExtension, project.container(ProviderInfo))

        project.task('pactVerify', description: 'Verify your pacts against your providers')

        project.afterEvaluate {
            project.pact.serviceProviders.all { ProviderInfo provider ->
                def providerTask = project.task("pactVerify_${provider.name}",
                    description: "Verify the pacts against ${provider.name}", type: PactVerificationTask) {
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
