package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Pact$
import au.com.dius.pact.model.Interaction
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.json4s.FileInput
import scala.collection.JavaConverters$

class PactPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('pact', PactPluginExtension)

        def providers = project.container(ProviderInfo)
        project.pact.extensions.serviceProviders = providers

        project.task('pactVerify') << {
            description 'Verify your pacts against your providers'
            AnsiConsole.out().println()
            def failures = [:]
            project.pact.serviceProviders.each { provider ->
                provider.consumers.each { consumer ->
                    Pact pact
                    if (consumer.pactFile instanceof File) {
                        pact = Pact$.MODULE$.from(new FileInput(consumer.pactFile))
                    } else {
                        throw new RuntimeException('You must specify the pactfile to execute (use pactFile = ...)')
                    }

                    AnsiConsole.out().println(Ansi.ansi().a('Verifying a pact between ').bold().a(consumer.name)
                        .boldOff().a(' and ').bold().a(provider.name).boldOff())
                    def interactions = JavaConverters$.MODULE$.asJavaIteratorConverter(pact.interactions().iterator())
                    interactions.asJava().each { Interaction interaction ->
                        AnsiConsole.out().println(Ansi.ansi().a('  ').a(interaction.description()))
                        try {
                            ProviderClient client = new ProviderClient(request: interaction.request(), provider: provider)

                            def response = client.makeRequest()

                            println response.dump()
                        } catch (e) {
                            AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
                                    .a(e.message).reset())
                            failures["Verifying a pact between ${consumer.name} and ${provider.name} ${provider.name} - ${interaction.description()}"] = e
                        }
                    }
                }
            }
            AnsiConsole.out().println()

            if (!failures.empty) {
                AnsiConsole.out().println('Failures:\n')
                failures.eachWithIndex { err, i ->
                    AnsiConsole.out().println("$i) ${err.key}")
                    AnsiConsole.out().println("    ${err.value}")
                    if (err.value instanceof Throwable) {
                        err.value.stackTrace.take(10).each {
                            AnsiConsole.out().println("      ${it}")
                        }
                    }
                    AnsiConsole.out().println()
                }

                throw new RuntimeException("There were ${failures.size()} pact failures")
            }
        }

    }
}
