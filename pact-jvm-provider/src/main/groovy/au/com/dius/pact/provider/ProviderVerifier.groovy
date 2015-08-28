package au.com.dius.pact.provider

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.v3.V3Pact
import au.com.dius.pact.model.v3.messaging.MessagePact
import org.apache.commons.lang3.StringUtils
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
@SuppressWarnings('UnusedImport')
import scala.collection.JavaConverters$

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
@SuppressWarnings('DuplicateStringLiteral')
class ProviderVerifier {

  static final String PACT_FILTER_CONSUMERS = 'pact.filter.consumers'
  static final String PACT_FILTER_DESCRIPTION = 'pact.filter.description'
  static final String PACT_FILTER_PROVIDERSTATE = 'pact.filter.providerState'

  Closure projectHasProperty
  Closure projectGetProperty
  String pactLoadFailureMessage
  Closure isBuildSpecificTask
  Closure executeBuildSpecificTask

  Map verifyProvider(ProviderInfo provider) {
    Map failures = [:]
    provider.consumers.findAll(this.&filterConsumers).each(this.&runVerificationForConsumer.curry(failures, provider))
    failures
  }

  void runVerificationForConsumer(Map failures, ProviderInfo provider, ConsumerInfo consumer) {
    AnsiConsole.out().println(Ansi.ansi().a('\nVerifying a pact between ').bold().a(consumer.name)
      .boldOff().a(' and ').bold().a(provider.name).boldOff())

    def pact = loadPactFileForConsumer(consumer)
    forEachInteraction(pact, this.&verifyInteraction.curry(provider, consumer, pact, failures))
  }

  List interactions(Pact pact) {
    if (pact instanceof V3Pact) {
      if (pact instanceof MessagePact) {
        pact.messages.findAll(this.&filterInteractions)
      } else {
        pact.interactions.findAll(this.&filterInteractions)
      }
    } else {
      JavaConverters$.MODULE$.seqAsJavaListConverter(pact.interactions()).asJava()
        .collect { new PactInteractionProxy(it) }.findAll(this.&filterInteractions)
    }
  }

  void forEachInteraction(Pact pact, Closure verifyInteraction) {
    List interactions = interactions(pact)
    if (interactions.empty) {
      AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
        .a('WARNING: Pact file has no interactions')
        .reset())
    } else {
      interactions.each(verifyInteraction)
    }
  }

  @SuppressWarnings('ThrowRuntimeException')
  Pact loadPactFileForConsumer(ConsumerInfo consumer) {
    if (consumer.pactFile instanceof URL) {
      AnsiConsole.out().println(Ansi.ansi().a("  [from URL ${consumer.pactFile}]"))
      new PactReader().loadPact(consumer.pactFile)
    } else if (consumer.pactFile instanceof File || pactFileExists(consumer.pactFile)) {
      AnsiConsole.out().println(Ansi.ansi().a("  [Using file ${consumer.pactFile}]"))
      new PactReader().loadPact(consumer.pactFile)
    } else {
      throw new RuntimeException(pactLoadFailureMessage(consumer))
    }
  }

  private boolean pactFileExists(def pactFile) {
    pactFile && new File(pactFile).exists()
  }

  boolean filterConsumers(def consumer) {
    !projectHasProperty(PACT_FILTER_CONSUMERS) ||
      consumer.name in projectGetProperty(PACT_FILTER_CONSUMERS).split(',')*.trim()
  }

  boolean filterInteractions(def interaction) {
    if (projectHasProperty(PACT_FILTER_DESCRIPTION) && projectHasProperty(PACT_FILTER_PROVIDERSTATE)) {
      matchDescription(interaction) && matchState(interaction)
    } else if (projectHasProperty(PACT_FILTER_DESCRIPTION)) {
      matchDescription(interaction)
    } else if (projectHasProperty(PACT_FILTER_PROVIDERSTATE)) {
      matchState(interaction)
    } else {
      true
    }
  }

  private boolean matchState(interaction) {
    if (interaction.providerState) {
      interaction.providerState ==~ projectGetProperty(PACT_FILTER_PROVIDERSTATE)
    } else {
      projectGetProperty(PACT_FILTER_PROVIDERSTATE).empty
    }
  }

  private boolean matchDescription(interaction) {
    interaction.description ==~ projectGetProperty(PACT_FILTER_DESCRIPTION)
  }

  @SuppressWarnings(['UnusedMethodParameter', 'EmptyIfStatement', 'EmptyElseBlock'])
  void verifyInteraction(ProviderInfo provider, ConsumerInfo consumer, def pact, Map failures, def interaction) {
    def interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name}" +
      " - ${interaction.description}"

    def stateChangeOk = true
    if (interaction.providerState) {
      stateChangeOk = stateChange(interaction.providerState, provider, consumer)
      if (stateChangeOk != true) {
        failures[interactionMessage] = stateChangeOk
        stateChangeOk = false
      } else {
        interactionMessage += " Given ${interaction.providerState}"
      }
    }

    if (stateChangeOk) {
      AnsiConsole.out().println(Ansi.ansi().a('  ').a(interaction.description))

      if (verificationType(provider, consumer) == PactVerification.REQUST_RESPONSE) {
        verifyResponseFromProvider(provider, interaction, interactionMessage, failures)
      } else {
//        verifyResponseByInvokingProviderMethods(pact, provider, consumer, interaction, interactionMessage)
      }
    }
  }

  private static PactVerification verificationType(ProviderInfo provider, ConsumerInfo consumer) {
    consumer.verificationType ?: provider.verificationType
  }

  @SuppressWarnings('PrintStackTrace')
  def stateChange(String state, ProviderInfo provider, ConsumerInfo consumer) {
    AnsiConsole.out().println(Ansi.ansi().a('  Given ').bold().a(state).boldOff())
    try {
      def stateChangeHandler = consumer.stateChange
      def stateChangeUsesBody = consumer.stateChangeUsesBody
      if (stateChangeHandler == null) {
        stateChangeHandler = provider.stateChangeUrl
        stateChangeUsesBody = provider.stateChangeUsesBody
      }
      if (stateChangeHandler == null || (stateChangeHandler instanceof String
        && StringUtils.isBlank(stateChangeHandler))) {
        AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.YELLOW)
          .a('WARNING: State Change ignored as there is no stateChange URL')
          .reset())
        return true
      } else if (stateChangeHandler instanceof Closure) {
        return stateChangeHandler.call(state)
      } else if (isBuildSpecificTask(stateChangeHandler)) {
        executeBuildSpecificTask(stateChangeHandler, state)
        return true
      }
      return executeHttpStateChangeRequest(stateChangeHandler, stateChangeUsesBody, state, provider)
    } catch (e) {
      AnsiConsole.out().println(Ansi.ansi().a('         ').fg(Ansi.Color.RED).a('State Change Request Failed - ')
        .a(e.message).reset())
      if (projectHasProperty('pact.showStacktrace')) {
        e.printStackTrace()
      }
      return e
    }
  }

  private executeHttpStateChangeRequest(stateChangeHandler, useBody, String state, ProviderInfo provider) {
    try {
      def url = stateChangeHandler instanceof URI ? stateChangeHandler
        : new URI(stateChangeHandler.toString())
      ProviderClient client = new ProviderClient(provider: provider)
      def response = client.makeStateChangeRequest(url, state, useBody)
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

  @SuppressWarnings('PrintStackTrace')
  void verifyResponseFromProvider(ProviderInfo provider, def interaction, String interactionMessage, Map failures) {
    try {
      ProviderClient client = new ProviderClient(request: interaction.request, provider: provider)

      def expectedResponse = interaction.response
      def actualResponse = client.makeRequest()

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
    } catch (e) {
      AnsiConsole.out().println(Ansi.ansi().a('      ').fg(Ansi.Color.RED).a('Request Failed - ')
        .a(e.message).reset())
      failures[interactionMessage] = e
      if (projectHasProperty('pact.showStacktrace')) {
        e.printStackTrace()
      }
    }
  }

  void verifyRequestResponsePact(Response expectedResponse, Map actualResponse, String interactionMessage,
                                 Map failures) {
    def comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
      actualResponse.statusCode, actualResponse.headers, actualResponse.data)

    AnsiConsole.out().println('    returns a response which')

    def s = ' returns a response which'
    displayMethodResult(failures, expectedResponse.status(), comparison.method, interactionMessage + s)
    displayHeadersResult(failures, expectedResponse.headers(), comparison.headers, interactionMessage + s)
    displayBodyResult(failures, comparison.body, interactionMessage + s)
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
}
