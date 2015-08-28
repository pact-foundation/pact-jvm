package au.com.dius.pact.provider.maven

import au.com.dius.pact.model.Interaction
@SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.Pact$
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ResponseComparison
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.json4s.FileInput
import org.json4s.StreamInput
import scala.collection.JavaConverters$

/**
 * Pact Verify Maven Plugin
 */
@Mojo(name = 'verify')
class PactProviderMojo extends AbstractMojo {

    private static final String PACT_FILTER_CONSUMERS = 'pact.filter.consumers'
    private static final String PACT_FILTER_DESCIPTION = 'pact.filter.description'
    private static final String PACT_FILTER_PROVIDERSTATE = 'pact.filter.providerState'

    @Parameter
    private List<Provider> serviceProviders

    @Parameter
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Map<String, String> configuration = [:]

    @Override
    @SuppressWarnings(['AbcMetric', 'ThrowRuntimeException', 'NestedBlockDepth', 'PrintStackTrace',
        'DuplicateStringLiteral', 'MethodSize'])
    void execute() throws MojoExecutionException, MojoFailureException {
        Map failures = [:]
        serviceProviders.each { provider ->
            List consumers = []
            consumers.addAll(provider.consumers)
            if (provider.pactFileDirectory != null) {
                consumers.addAll(loadPactFiles(provider, provider.pactFileDirectory))
            }

            consumers.findAll(this.&filterConsumers).each { consumer ->
                AnsiConsole.out().println(Ansi.ansi().a('\nVerifying a pact between ').bold().a(consumer.name)
                    .boldOff().a(' and ').bold().a(provider.name).boldOff())

                Pact pact
                if (consumer.pactFile) {
                    AnsiConsole.out().println(Ansi.ansi().a("  [Using file ${consumer.pactFile}]"))
                    pact = Pact$.MODULE$.from(new FileInput(consumer.pactFile))
                } else if (consumer.pactUrl) {
                    AnsiConsole.out().println(Ansi.ansi().a("  [from URL ${consumer.pactUrl}]"))
                    pact = Pact$.MODULE$.from(new StreamInput(consumer.pactUrl.newInputStream(requestProperties:
                        ['Accept': 'application/json'])))
                } else {
                    throw new RuntimeException('You must specify the pactfile to execute for consumer ' +
                        "'${consumer.name}' (use <pactFile> or <pactUrl>)")
                }

                def interactions = JavaConverters$.MODULE$.seqAsJavaListConverter(pact.interactions())
                interactions.asJava().findAll(this.&filterInteractions).each { Interaction interaction ->
                    def interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name} " +
                        "- ${interaction.description()}"

                    def stateChangeOk = true
                    if (interaction.providerState.defined) {
                        stateChangeOk = stateChange(interaction.providerState.get(), provider, consumer)
                        if (stateChangeOk != true) {
                            failures[interactionMessage] = stateChangeOk
                            stateChangeOk = false
                        } else {
                            interactionMessage += " Given ${interaction.providerState.get()}"
                        }
                    }

                    if (stateChangeOk) {
                        AnsiConsole.out().println(Ansi.ansi().a('  ').a(interaction.description()))

                        try {
                            ProviderClient client = new ProviderClient(request: interaction.request(),
                                provider: provider)

                            def expectedResponse = interaction.response()
                            def actualResponse = client.makeRequest()

                            def comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
                                actualResponse.statusCode, actualResponse.headers, actualResponse.data)

                            AnsiConsole.out().println('    returns a response which')

                            def s = ' returns a response which'
                            displayMethodResult(failures, expectedResponse.status(), comparison.method,
                                interactionMessage + s)
                            displayHeadersResult(failures, expectedResponse.headers(), comparison.headers,
                                interactionMessage + s)
                            expectedResponse.body().defined ? expectedResponse.body().get() : ''
                            displayBodyResult(failures, comparison.body, interactionMessage + s)
                        } catch (e) {
                            AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
                                .a(e.message).reset())
                            failures[interactionMessage] = e
                            if (propertyDefined('pact.showStacktrace')) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        if (failures.size() > 0) {
            AnsiConsole.out().println('\nFailures:\n')
            failures.eachWithIndex { err, i ->
                AnsiConsole.out().println("$i) ${err.key}")
                if (err.value instanceof Exception || err.value instanceof Error) {
                    err.value.message.split('\n').each {
                        AnsiConsole.out().println("      $it")
                    }
                } else if (err.value instanceof Map && err.value.containsKey('diff')) {
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
                } else {
                    err.value.each { key, message ->
                        AnsiConsole.out().println("      $key -> $message")
                    }
                }
                AnsiConsole.out().println()
            }

            throw new RuntimeException("There were ${failures.size()} pact failures")
        }
    }

    @SuppressWarnings('ThrowRuntimeException')
    List loadPactFiles(def provider, File pactFileDir) {
        if (!pactFileDir.exists()) {
            throw new RuntimeException("Pact file directory ($pactFileDir) does not exist")
        }

        if (!pactFileDir.isDirectory()) {
            throw new RuntimeException("Pact file directory ($pactFileDir) is not a directory")
        }

        if (!pactFileDir.canRead()) {
            throw new RuntimeException("Pact file directory ($pactFileDir) is not readable")
        }

        AnsiConsole.out().println("Loading pact files for provider ${provider.name} from $pactFileDir")

        List consumers = []
        pactFileDir.eachFileMatch FileType.FILES, ~/.*\.json/, {
            def pactJson = new JsonSlurper().parse(it)
            if (pactJson.provider.name == provider.name) {
                consumers << new Consumer(name: pactJson.consumer.name, pactFile: it)
            }
        }
        consumers
    }

    @SuppressWarnings('DuplicateStringLiteral')
    void displayMethodResult(Map failures, int status, def comparison, String comparisonDescription) {
        def ansi = Ansi.ansi().a('      ').a('has status code ').bold().a(status).boldOff().a(' (')
        if (comparison == true) {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
        } else {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
            failures["$comparisonDescription has status code $status"] = comparison
        }
    }

    @SuppressWarnings('DuplicateStringLiteral')
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

    @SuppressWarnings('DuplicateStringLiteral')
    void displayBodyResult(Map failures, def comparison, String comparisonDescription) {
        def ansi = Ansi.ansi().a('      ').a('has a matching body').a(' (')
        if (comparison.isEmpty()) {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.GREEN).a('OK').reset().a(')'))
        } else {
            AnsiConsole.out().println(ansi.fg(Ansi.Color.RED).a('FAILED').reset().a(')'))
            failures["$comparisonDescription has a matching body"] = comparison
        }
    }

    @SuppressWarnings(['DuplicateStringLiteral', 'PrintStackTrace'])
    def stateChange(String state, Provider provider, Consumer consumer) {
        AnsiConsole.out().println(Ansi.ansi().a('  Given ').bold().a(state).boldOff())
        try {
            if (consumer.stateChangeUrl == null && provider.stateChangeUrl == null) {
                AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
                    .a('WARNING: Provider State ignored as there is no stateChange URL defined'))
            } else {
                def stateChangeUrl = consumer.stateChangeUrl
                def stateChangeUsesBody = consumer.stateChangeUsesBody
                if (stateChangeUrl == null) {
                    stateChangeUrl = provider.stateChangeUrl
                    stateChangeUsesBody = provider.stateChangeUsesBody
                }
                ProviderClient client = new ProviderClient(provider: provider)
                def response = client.makeStateChangeRequest(stateChangeUrl.toString(), state, stateChangeUsesBody)
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
            return true
        } catch (e) {
            AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED).a('State Change Request Failed - ')
                .a(e.message).reset())
            if (propertyDefined('pact.showStacktrace')) {
                e.printStackTrace()
            }
            return e
        }
    }

    private boolean propertyDefined(String key) {
        System.getProperty(key) != null || configuration.containsKey(key)
    }

    private boolean filterConsumers(def consumer) {
        !this.propertyDefined(PACT_FILTER_CONSUMERS) ||
            consumer.name in this.property(PACT_FILTER_CONSUMERS).split(',')*.trim()
    }

    private String property(String key) {
        System.getProperty(key, configuration.get(key))
    }

    private boolean filterInteractions(def interaction) {
        if (propertyDefined(PACT_FILTER_DESCIPTION) && propertyDefined(PACT_FILTER_PROVIDERSTATE)) {
            matchDescription(interaction) && matchState(interaction)
        } else if (propertyDefined(PACT_FILTER_DESCIPTION)) {
            matchDescription(interaction)
        } else if (propertyDefined(PACT_FILTER_PROVIDERSTATE)) {
            matchState(interaction)
        } else {
            true
        }
    }

    private boolean matchState(def interaction) {
        if (interaction.providerState().defined) {
            interaction.providerState().get() ==~ property(PACT_FILTER_PROVIDERSTATE)
        } else {
            property(PACT_FILTER_PROVIDERSTATE).empty
        }
    }

    private boolean matchDescription(def interaction) {
        interaction.description() ==~ property(PACT_FILTER_DESCIPTION)
    }
}
