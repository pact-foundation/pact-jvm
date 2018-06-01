package au.com.dius.pact.provider

import au.com.dius.pact.model.FilteredPact
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.UrlPactSource
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.provider.reporters.AnsiConsoleReporter
import au.com.dius.pact.provider.reporters.VerifierReporter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import scala.Function1

import java.lang.reflect.Method
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

import static au.com.dius.pact.provider.ProviderVerifierKt.reportVerificationResults

/**
 * Verifies the providers against the defined consumers in the context of a build plugin
 */
@Slf4j
class ProviderVerifier extends ProviderVerifierBase {

  static final public String PACT_FILTER_CONSUMERS = 'pact.filter.consumers'
  static final public String PACT_FILTER_DESCRIPTION = 'pact.filter.description'
  static final public String PACT_FILTER_PROVIDERSTATE = 'pact.filter.providerState'
  static final public String PACT_SHOW_STACKTRACE = 'pact.showStacktrace'
  static final public String PACT_SHOW_FULLDIFF = 'pact.showFullDiff'

  def pactLoadFailureMessage
  Function<Object, Boolean> isBuildSpecificTask = { null }
  BiConsumer<Object, ProviderState> executeBuildSpecificTask = { } as BiConsumer<Object, ProviderState>
  Supplier<URL[]> projectClasspath = { }
  List<VerifierReporter> reporters = [ new AnsiConsoleReporter() ]
  Function<Method, Object> providerMethodInstance = { Method m -> m.declaringClass.newInstance() }
  Supplier<String> providerVersion = { System.getProperty('pact.provider.version') }

  Map verifyProvider(ProviderInfo provider) {
    Map failures = [:]

    initialiseReporters(provider)

    def consumers = provider.consumers.findAll(this.&filterConsumers)
    if (consumers.empty) {
      reporters.each { it.warnProviderHasNoConsumers(provider) }
    }

    consumers.each(this.&runVerificationForConsumer.curry(failures, provider))

    failures
  }

  void initialiseReporters(ProviderInfo provider) {
    reporters.each {
      if (it.hasProperty('displayFullDiff')) {
        it.displayFullDiff = projectHasProperty.apply(PACT_SHOW_FULLDIFF)
      }
      it.initialise(provider)
    }
  }

  @CompileStatic
  void runVerificationForConsumer(Map failures, ProviderInfo provider, ConsumerInfo consumer,
                                  PactBrokerClient client = null) {
    reportVerificationForConsumer(consumer, provider)
    FilteredPact pact = new FilteredPact(loadPactFileForConsumer(consumer),
      this.&filterInteractions as Predicate<Interaction>)
    if (pact.interactions.empty) {
      reporters.each { it.warnPactFileHasNoInteractions(pact) }
    } else {
      boolean result = pact.interactions
        .collect(this.&verifyInteraction.curry(provider, consumer, failures))
        .inject(true) { acc, val -> acc && val }
      if (pact.isFiltered()) {
        log.warn('Skipping publishing of verification results as the interactions have been filtered')
      } else if (publishingResultsDisabled()) {
        log.warn('Skipping publishing of verification results as it has been disabled ' +
          "(${PACT_VERIFIER_PUBLISHRESUTS} is false)")
      } else {
        reportVerificationResults(pact, result, providerVersion?.get() ?: '0.0.0', client)
      }
    }
  }

  void reportVerificationForConsumer(ConsumerInfo consumer, ProviderInfo provider) {
    reporters.each { it.reportVerificationForConsumer(consumer, provider) }
  }

  @SuppressWarnings('ThrowRuntimeException')
  Pact<? extends Interaction> loadPactFileForConsumer(ConsumerInfo consumer) {
    def pactSource = consumer.pactSource
    if (pactSource instanceof Closure) {
      pactSource = pactSource.call()
    }

    if (pactSource instanceof UrlPactSource) {
      reporters.each { it.verifyConsumerFromUrl(pactSource, consumer) }
      def options = [:]
      if (consumer.pactFileAuthentication) {
        options.authentication = consumer.pactFileAuthentication
      }
      PactReader.loadPact(options, pactSource)
    } else {
      try {
        def pact = PactReader.loadPact(pactSource)
        reporters.each { it.verifyConsumerFromFile(pact.source, consumer) }
        pact
      } catch (e) {
        log.error('Failed to load pact file', e)
        String message = generateLoadFailureMessage(consumer)
        reporters.each { it.pactLoadFailureForConsumer(consumer, message) }
        throw new RuntimeException(message)
      }
    }
  }

  private generateLoadFailureMessage(ConsumerInfo consumer) {
    if (pactLoadFailureMessage instanceof Closure) {
      pactLoadFailureMessage.call(consumer) as String
    } else if (pactLoadFailureMessage instanceof Function) {
      pactLoadFailureMessage.apply(consumer) as String
    } else if (pactLoadFailureMessage instanceof Function1) {
      pactLoadFailureMessage.apply(consumer) as String
    } else {
      pactLoadFailureMessage as String
    }
  }

  boolean filterConsumers(def consumer) {
    !projectHasProperty.apply(PACT_FILTER_CONSUMERS) ||
      consumer.name in projectGetProperty.apply(PACT_FILTER_CONSUMERS).split(',')*.trim()
  }

  boolean filterInteractions(def interaction) {
    if (projectHasProperty.apply(PACT_FILTER_DESCRIPTION) && projectHasProperty.apply(PACT_FILTER_PROVIDERSTATE)) {
      matchDescription(interaction) && matchState(interaction)
    } else if (projectHasProperty.apply(PACT_FILTER_DESCRIPTION)) {
      matchDescription(interaction)
    } else if (projectHasProperty.apply(PACT_FILTER_PROVIDERSTATE)) {
      matchState(interaction)
    } else {
      true
    }
  }

  private boolean matchState(interaction) {
    if (interaction.providerStates) {
      interaction.providerStates.any { it.name ==~ projectGetProperty.apply(PACT_FILTER_PROVIDERSTATE) }
    } else {
      projectGetProperty.apply(PACT_FILTER_PROVIDERSTATE).empty
    }
  }

  private boolean matchDescription(interaction) {
    interaction.description ==~ projectGetProperty.apply(PACT_FILTER_DESCRIPTION)
  }

  boolean verifyInteraction(ProviderInfo provider, ConsumerInfo consumer, Map failures, def interaction) {
    def interactionMessage = "Verifying a pact between ${consumer.name} and ${provider.name}" +
      " - ${interaction.description}"

    ProviderClient providerClient = new ProviderClient(provider, new HttpClientFactory())
    def stateChangeResult = StateChange.executeStateChange(this, provider, consumer, interaction, interactionMessage,
      failures, providerClient)
    if (stateChangeResult.stateChangeOk) {
      interactionMessage += stateChangeResult.message
      reportInteractionDescription(interaction)

      boolean result = false
      if (ProviderUtils.verificationType(provider, consumer) == PactVerification.REQUST_RESPONSE) {
        log.debug('Verifying via request/response')
        result = verifyResponseFromProvider(provider, interaction, interactionMessage, failures, providerClient)
      } else {
        log.debug('Verifying via annotated test method')
        result = verifyResponseByInvokingProviderMethods(provider, consumer, interaction, interactionMessage, failures)
      }

      if (provider.stateChangeTeardown) {
        StateChange.executeStateChangeTeardown(this, interaction, provider, consumer, providerClient)
      }

      result
    } else {
      false
    }
  }

  void reportInteractionDescription(interaction) {
    reporters.each { it.interactionDescription(interaction) }
  }

  void reportStateForInteraction(String state, ProviderInfo provider, ConsumerInfo consumer, boolean isSetup) {
    reporters.each { it.stateForInteraction(state, provider, consumer, isSetup) }
  }

  boolean verifyResponseFromProvider(ProviderInfo provider, Interaction interaction, String interactionMessage,
                                     Map failures,
                                     ProviderClient client) {
    try {
      def expectedResponse = interaction.response
      def actualResponse = client.makeRequest(interaction.request.generatedRequest())

      verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
    } catch (e) {
      failures[interactionMessage] = e
      reporters.each {
        it.requestFailed(provider, interaction, interactionMessage, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE))
      }
      false
    }
  }

  boolean verifyRequestResponsePact(Response expectedResponse, Map actualResponse, String interactionMessage,
                                 Map failures) {
    def comparison = ResponseComparison.compareResponse(expectedResponse, actualResponse,
      actualResponse.statusCode, actualResponse.headers, actualResponse.data)

    reporters.each { it.returnsAResponseWhich() }

    def s = ' returns a response which'
    def result = true
    result &= displayStatusResult(failures, expectedResponse.status, comparison.method, interactionMessage + s)
    result &= displayHeadersResult(failures, expectedResponse.headers, comparison.headers, interactionMessage + s)
    result &= displayBodyResult(failures, comparison.body, interactionMessage + s)
    result
  }

  boolean displayStatusResult(Map failures, int status, def comparison, String comparisonDescription) {
    if (comparison == true) {
      reporters.each { it.statusComparisonOk(status) }
      true
    } else {
      reporters.each { it.statusComparisonFailed(status, comparison) }
      failures["$comparisonDescription has status code $status"] = comparison
      false
    }
  }

  boolean displayHeadersResult(Map failures, def expected, Map comparison, String comparisonDescription) {
    if (comparison.isEmpty()) {
      true
    } else {
      reporters.each { it.includesHeaders() }
      Map expectedHeaders = expected
      boolean result = true
      comparison.each { key, headerComparison ->
        def expectedHeaderValue = expectedHeaders[key]
        if (headerComparison == true) {
          reporters.each { it.headerComparisonOk(key, expectedHeaderValue) }
        } else {
          reporters.each { it.headerComparisonFailed(key, expectedHeaderValue, headerComparison) }
          failures["$comparisonDescription includes headers \"$key\" with value \"$expectedHeaderValue\""] =
            headerComparison
          result = false
        }
      }
      result
    }
  }

  boolean displayBodyResult(Map failures, def comparison, String comparisonDescription) {
    if (comparison.isEmpty()) {
      reporters.each { it.bodyComparisonOk() }
      true
    } else {
      reporters.each { it.bodyComparisonFailed(comparison) }
      failures["$comparisonDescription has a matching body"] = comparison
      false
    }
  }

  @SuppressWarnings(['ThrowRuntimeException', 'ParameterCount'])
  boolean verifyResponseByInvokingProviderMethods(ProviderInfo providerInfo, ConsumerInfo consumer,
                                               def interaction, String interactionMessage, Map failures) {
    try {
      def urls = projectClasspath.get()
      URLClassLoader loader = new URLClassLoader(urls, GroovyObject.classLoader)
      def configurationBuilder = new ConfigurationBuilder()
        .setScanners(new MethodAnnotationsScanner())
        .addClassLoader(loader)
        .addUrls(loader.URLs)

      def scan = ProviderUtils.packagesToScan(providerInfo, consumer)
      if (!scan.empty) {
        def filterBuilder = new FilterBuilder()
        scan.each { filterBuilder.include(it) }
        configurationBuilder.filterInputsBy(filterBuilder)
      }

      Reflections reflections = new Reflections(configurationBuilder)
      def methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(PactVerifyProvider)
      def providerMethods = methodsAnnotatedWith.findAll { Method m ->
        log.debug("Found annotated method $m")
        def annotation = m.annotations.find { it.annotationType().toString() == PactVerifyProvider.toString() }
        log.debug("Found annotation $annotation")
        annotation?.value() == interaction.description
      }

      if (providerMethods.empty) {
        reporters.each { it.errorHasNoAnnotatedMethodsFoundForInteraction(interaction) }
        throw new RuntimeException('No annotated methods were found for interaction ' +
          "'${interaction.description}'. You need to provide a method annotated with " +
          "@PactVerifyProvider(\"${interaction.description}\") that returns the message contents.")
      } else {
        if (interaction instanceof Message) {
          verifyMessagePact(providerMethods, interaction as Message, interactionMessage, failures)
        } else {
          def expectedResponse = interaction.response
          boolean result = true
          providerMethods.each {
            def actualResponse = invokeProviderMethod(it)
            result &= verifyRequestResponsePact(expectedResponse, actualResponse, interactionMessage, failures)
          }
          result
        }
      }
    } catch (e) {
      failures[interactionMessage] = e
      reporters.each { it.verificationFailed(interaction, e, projectHasProperty.apply(PACT_SHOW_STACKTRACE)) }
      false
    }
  }

  boolean verifyMessagePact(Set methods, Message message, String interactionMessage, Map failures) {
    boolean result = true
    methods.each {
      reporters.each { it.generatesAMessageWhich() }
      def actualMessage = OptionalBody.body(invokeProviderMethod(it, providerMethodInstance.apply(it)) as String)
      def comparison = ResponseComparison.compareMessage(message, actualMessage)
      def s = ' generates a message which'
      result &= displayBodyResult(failures, comparison, interactionMessage + s)
    }
    result
  }

  @SuppressWarnings('ThrowRuntimeException')
  static invokeProviderMethod(Method m, Object instance) {
    try {
      m.invoke(instance)
    } catch (e) {
      throw new RuntimeException("Failed to invoke provider method '${m.name}'", e)
    }
  }

  void displayFailures(Map failures) {
    reporters.each { it.displayFailures(failures) }
  }

  void finialiseReports() {
    reporters.each { it.finaliseReport() }
  }
}
