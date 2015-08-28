package au.com.dius.pact.provider.gradle

@SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.v3.V3Pact
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.PactInteractionProxy
import au.com.dius.pact.provider.PactVerification
import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ResponseComparison
import org.apache.commons.lang3.StringUtils
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import scala.collection.JavaConverters$

import java.lang.reflect.Method

/**
 * Task to verify a pact against a provider
 */
@SuppressWarnings(['DuplicateImport', 'DuplicateStringLiteral'])
class PactVerificationTask extends DefaultTask {

    ProviderInfo providerToVerify

    @TaskAction
    void verifyPact() {
        ext.failures = [:]

        providerToVerify.consumers
            .findAll(this.&filterConsumers).each(this.&runVerificationForConsumer.curry(providerToVerify))

        if (ext.failures.size() > 0) {
            displayFailures(ext.failures)
            throw new GradleScriptException(
                "There were ${ext.failures.size()} pact failures for provider ${providerToVerify.name}", null)
        }
    }

    void runVerificationForConsumer(ProviderInfo provider, ConsumerInfo consumer) {
        AnsiConsole.out().println(Ansi.ansi().a('\nVerifying a pact between ').bold().a(consumer.name)
            .boldOff().a(' and ').bold().a(provider.name).boldOff())

        def pact = loadPactFileForConsumer(consumer)

        def interactions
        if (pact instanceof V3Pact) {
            if (pact instanceof MessagePact) {
                interactions = pact.messages.findAll(this.&filterInteractions)
            } else {
                interactions = pact.interactions.findAll(this.&filterInteractions)
            }
        } else {
            interactions = JavaConverters$.MODULE$.seqAsJavaListConverter(pact.interactions()).asJava()
                .collect { new PactInteractionProxy(it) }.findAll(this.&filterInteractions)
        }

        if (interactions.empty) {
            AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
                .a('WARNING: Pact file has no interactions')
                .reset())
        } else {
            interactions.each(this.&verifyInteraction.curry(provider, consumer, pact))
        }
    }

    private void displayFailures(failures) {
        AnsiConsole.out().println('\nFailures:\n')
        failures.eachWithIndex { err, i ->
            AnsiConsole.out().println("$i) ${err.key}")
            if (err.value instanceof Throwable) {
                displayError(err.value)
            } else if (err.value instanceof Map && err.value.containsKey('diff')) {
                displayDiff(err)
            } else if (err.value instanceof String) {
                AnsiConsole.out().println("      ${err.value}")
            } else {
                err.value.each { key, message ->
                    AnsiConsole.out().println("      $key -> $message")
                }
            }
            AnsiConsole.out().println()
        }
    }

    void displayDiff(err) {
        err.value.comparison.each { key, message ->
            AnsiConsole.out().println("      $key -> $message")
        }

        AnsiConsole.out().println()
        AnsiConsole.out().println('      Diff:')
        AnsiConsole.out().println()

        err.value.diff.each { delta ->
            if (delta.startsWith('@')) {
                AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.CYAN).a(delta).reset())
            } else if (delta.startsWith('-')) {
                AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a(delta).reset())
            } else if (delta.startsWith('+')) {
                AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.GREEN).a(delta).reset())
            } else {
                AnsiConsole.out().println("      $delta")
            }
        }
    }

    void displayError(Throwable err) {
        err.message.split('\n').each {
            AnsiConsole.out().println("      $it")
        }
    }

    void verifyInteraction(ProviderInfo provider, ConsumerInfo consumer, def pact, def interaction) {
        def interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name}" +
            " - ${interaction.description}"

        def stateChangeOk = true
        if (interaction.providerState) {
            stateChangeOk = stateChange(interaction.providerState, consumer)
            if (stateChangeOk != true) {
                ext.failures[interactionMessage] = stateChangeOk
                stateChangeOk = false
            } else {
                interactionMessage += " Given ${interaction.providerState}"
            }
        }

        if (stateChangeOk) {
            AnsiConsole.out().println(Ansi.ansi().a('  ').a(interaction.description))

            if (verificationType(provider, consumer) == PactVerification.REQUST_RESPONSE) {
                verifyResponseFromProvider(provider, interaction, interactionMessage)
            } else {
                verifyResponseByInvokingProviderMethods(pact, provider, consumer, interaction, interactionMessage)
            }
        }
    }

    private PactVerification verificationType(ProviderInfo provider, ConsumerInfo consumer) {
        consumer.verificationType ?: provider.verificationType
    }

    @SuppressWarnings(['PrintStackTrace', 'ThrowRuntimeException'])
    void verifyResponseByInvokingProviderMethods(def pact, ProviderInfo providerInfo, ConsumerInfo consumer,
                                                 def interaction, String interactionMessage) {
        try {
            def urls = project.sourceSets.test.runtimeClasspath*.toURL() as URL[]
            URLClassLoader loader = new URLClassLoader(urls, GroovyObject.classLoader)
            def configurationBuilder = new ConfigurationBuilder()
                .setScanners(new MethodAnnotationsScanner())
                .addClassLoader(loader)
                .addUrls(loader.URLs)

          def scan = packagesToScan(providerInfo, consumer)
          if (!scan.empty) {
                def filterBuilder = new FilterBuilder()
                scan.each { filterBuilder.include(it) }
                configurationBuilder.filterInputsBy(filterBuilder)
            }

            Reflections reflections = new Reflections(configurationBuilder)
            def methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(PactVerifyProvider)
            def providerMethods = methodsAnnotatedWith.findAll { Method m ->
                logger.debug("Found annotated method $m")
                def annotation = m.annotations.find { it.annotationType().toString() == PactVerifyProvider.toString() }
                logger.debug("Found annotation $annotation")
                annotation?.value() == interaction.description
            }

            if (providerMethods.empty) {
                throw new RuntimeException('No annotated methods were found for interaction ' +
                    "'${interaction.description}'")
            } else {
                if (pact instanceof MessagePact) {
                    verifyMessagePact(providerMethods, interaction as Message, interactionMessage)
                } else {
                    def expectedResponse = interaction.response
                    providerMethods.each {
                        def actualResponse = invokeProviderMethod(it)
                        verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage)
                    }
                }
            }
        } catch (e) {
            AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Verification Failed - ')
                .a(e.message).reset())
            ext.failures[interactionMessage] = e
            if (project.hasProperty('pact.showStacktrace')) {
                e.printStackTrace()
            }
        }
    }

    private List packagesToScan(ProviderInfo providerInfo, ConsumerInfo consumer) {
        consumer.packagesToScan ?: providerInfo.packagesToScan
    }

    void verifyRequestResponsePact(Response expectedResponse, Map actualResponse, String interactionMessage) {
        def comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
            actualResponse.statusCode, actualResponse.headers, actualResponse.data)

        AnsiConsole.out().println('    returns a response which')

        def s = ' returns a response which'
        displayMethodResult(ext.failures, expectedResponse.status(), comparison.method,
            interactionMessage + s)
        displayHeadersResult(ext.failures, expectedResponse.headers(), comparison.headers,
            interactionMessage + s)
        expectedResponse.body().defined ? expectedResponse.body().get() : ''
        displayBodyResult(ext.failures, comparison.body, interactionMessage + s)
    }

    void verifyMessagePact(Set methods, Message message, String interactionMessage) {
        methods.each {
            AnsiConsole.out().println('    generates a message which')
            def actualMessage = invokeProviderMethod(it)
            def comparison = ResponseComparison.compareMessage(message, actualMessage)
            def s = ' generates a message which'
            displayBodyResult(ext.failures, comparison, interactionMessage + s)
        }
    }

    def invokeProviderMethod(Method m) {
        m.invoke(m.declaringClass.newInstance())
    }

    @SuppressWarnings('PrintStackTrace')
    void verifyResponseFromProvider(ProviderInfo provider, def interaction, String interactionMessage) {
        try {
            ProviderClient client = new ProviderClient(request: interaction.request, provider: provider)

            def expectedResponse = interaction.response
            def actualResponse = client.makeRequest()

            verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage)
        } catch (e) {
            AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
                .a(e.message).reset())
            ext.failures[interactionMessage] = e
            if (project.hasProperty('pact.showStacktrace')) {
                e.printStackTrace()
            }
        }
    }

    private loadPactFileForConsumer(ConsumerInfo consumer) {
        if (consumer.pactFile instanceof File) {
            AnsiConsole.out().println(Ansi.ansi().a("  [Using file ${consumer.pactFile}]"))
            new PactReader().loadPact(consumer.pactFile)
        } else if (consumer.pactFile instanceof URL) {
            AnsiConsole.out().println(Ansi.ansi().a("  [from URL ${consumer.pactFile}]"))
            new PactReader().loadPact(consumer.pactFile)
        } else {
            throw new GradleScriptException('You must specify the pactfile to execute (use pactFile = ...)',
                null)
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
            Map expectedHeaders = JavaConverters$.MODULE$.mapAsJavaMapConverter(expected.get()).asJava()
            comparison.each { key, headerComparison ->
                def expectedHeaderValue = expectedHeaders[key]
                def ansi = Ansi.ansi().a('        "').bold().a(key).boldOff().a('" with value "').bold()
                    .a(expectedHeaderValue).boldOff().a('" (')
                if (headerComparison == true) {
                    AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
                } else {
                    AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
                    failures["$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""] =
                        headerComparison
                }
            }
        }
    }

    void displayBodyResult(Map failures, def comparison, String comparisonDescription) {
        def ansi = Ansi.ansi().a('      ').a('has a matching body').a(' (')
        if (comparison.isEmpty()) {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
        } else {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
            failures["$comparisonDescription has a matching body"] = comparison
        }
    }

    @SuppressWarnings('PrintStackTrace')
    def stateChange(String state, ConsumerInfo consumer) {
        AnsiConsole.out().println(Ansi.ansi().a('  Given ').bold().a(state).boldOff())
        try {
            def stateChangeHandler = consumer.stateChange
            if (stateChangeHandler == null || (stateChangeHandler instanceof String
                && StringUtils.isBlank(stateChangeHandler))) {
              AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
                .a('WARNING: State Change ignored as there is no stateChange URL')
                .reset())
              return true
            } else if (stateChangeHandler instanceof Closure) {
              return stateChangeHandler.call(state)
            } else if (stateChangeHandler instanceof Task || stateChangeHandler instanceof String
              && project.tasks.findByName(stateChangeHandler)) {
              def task = stateChangeHandler instanceof String ? project.tasks.getByName(stateChangeHandler)
                  : stateChangeHandler
              task.setProperty('providerState', state)
              task.ext.providerState = state
              def build = project.task(type: GradleBuild) {
                tasks = [task.name]
              }
              build.execute()
              return true
            }
            return executeHttpStateChangeRequest(stateChangeHandler, state, consumer)
        } catch (e) {
            AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED).a('State Change Request Failed - ')
                .a(e.message).reset())
            if (project.hasProperty('pact.showStacktrace')) {
                e.printStackTrace()
            }
            return e
        }
    }

    private executeHttpStateChangeRequest(def stateChangeHandler, String state, ConsumerInfo consumer) {
        try {
            def url = stateChangeHandler instanceof URI ? stateChangeHandler
                : new URI(stateChangeHandler.toString())
            ProviderClient client = new ProviderClient(provider: providerToVerify)
            def response = client.makeStateChangeRequest(url, state, consumer.stateChangeUsesBody)
            if (response) {
                try {
                    if (response.statusLine.statusCode >= 400) {
                        AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED)
                            .a('State Change Request Failed - ')
                            .a(response.statusLine.toString()).reset())
                        return 'State Change Request Failed - ' + response.statusLine.toString()
                    }
                } finally {
                    response.close()
                }
            }
        } catch (URISyntaxException ex) {
            AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
                .a("WARNING: State Change ignored as there is no stateChange URL, received \"$stateChangeHandler\"")
                .reset())
        }
        true
    }

    boolean filterConsumers(def consumer) {
        !project.hasProperty('pact.filter.consumers') || consumer.name in project.property('pact.filter.consumers')
            .split(',')*.trim()
    }

  boolean filterInteractions(def interaction) {
    if (project.hasProperty('pact.filter.description') && project.hasProperty('pact.filter.providerState')) {
      matchDescription(interaction) && matchState(interaction)
    } else if (project.hasProperty('pact.filter.description')) {
      matchDescription(interaction)
    } else if (project.hasProperty('pact.filter.providerState')) {
      matchState(interaction)
    } else {
      true
    }
  }

  private boolean matchState(interaction) {
    if (interaction.providerState) {
      interaction.providerState ==~ project.property('pact.filter.providerState')
    } else {
      project.property('pact.filter.providerState').empty
    }
  }

  private boolean matchDescription(interaction) {
    interaction.description ==~ project.property('pact.filter.description')
  }
}
