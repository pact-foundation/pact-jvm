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

        project.task('pactVerify', description: 'Verify your pacts against your providers')

        project.afterEvaluate {
            providers.all { ProviderInfo provider ->
                def providerTask = project.task("pactVerify_${provider.name}", description: "Verify the pacts against ${provider.name}")
                providerTask << {
                    ext.failures = [:]
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
                            def interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name} ${provider.name} - ${interaction.description()}"
                            try {
                                ProviderClient client = new ProviderClient(request: interaction.request(), provider: provider)

                                def expectedResponse = interaction.response()
                                def actualResponse = client.makeRequest()
                                def comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse)

                                AnsiConsole.out().println('    returns a response which')
                                displayMethodResult(failures, expectedResponse.status(), comparison.method,
                                    interactionMessage + ' returns a response which')
                                displayHeadersResult(failures, expectedResponse.headers(), comparison.headers,
                                    interactionMessage + ' returns a response which')
                                def expectedBody = expectedResponse.body().defined ? expectedResponse.bodyString().get() : ''
                                displayBodyResult(failures, expectedBody, comparison.body,
                                    interactionMessage + ' returns a response which')
                            } catch (e) {
                                AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
                                    .a(e.message).reset())
                                ext.failures[interactionMessage] = e
                                if (project.hasProperty('pact.showStacktrace')) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    if (ext.failures.size() > 0) {
                        AnsiConsole.out().println('Failures:\n')
                        failures.eachWithIndex { err, i ->
                            AnsiConsole.out().println("$i) ${err.key}")
                            err.value.message.split('\n').each {
                                AnsiConsole.out().println("      $it")
                            }
                            AnsiConsole.out().println()
                        }

                        throw new RuntimeException("There were ${failures.size()} pact failures for provider ${provider.name}")
                    }
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

    void displayMethodResult(Map failures, int status, def comparison, String comparisonDescription) {
        def ansi = Ansi.ansi().a('      ').a('has status code ').bold().a(status).boldOff().a(' (')
        if (comparison == true) {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
        } else {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
            failures["$comparisonDescription has status code $status"] = comparison
        }
    }

    void displayHeadersResult(Map failures, def expected, Map comparison, String comparisonDescription) {
        if (!comparison.isEmpty()) {
            AnsiConsole.out().println('      includes headers')
            Map expectedHeaders = JavaConverters$.MODULE$.asJavaMapConverter(expected.get()).asJava()
            comparison.each { key, headerComparison ->
                def expectedHeaderValue = expectedHeaders[key]
                def ansi = Ansi.ansi().a('        "').bold().a(key).boldOff().a('" with value "').bold()
                    .a(expectedHeaderValue).boldOff().a('" (')
                if (headerComparison == true) {
                    AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
                } else {
                    AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
                    failures["$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""] = headerComparison
                }
            }
        }
    }

    void displayBodyResult(Map failures, String body, def comparison, String comparisonDescription) {
        def ansi = Ansi.ansi().a('      ').a('has body').a(' (')
        if (comparison == true) {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
        } else {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
            failures["$comparisonDescription has body"] = comparison
        }
    }
}
