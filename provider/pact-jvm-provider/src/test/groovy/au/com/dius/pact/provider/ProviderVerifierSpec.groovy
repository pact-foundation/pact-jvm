package au.com.dius.pact.provider

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.UnknownPactSource
import au.com.dius.pact.core.model.UrlSource
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.pactbroker.TestResult
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.provider.reporters.VerifierReporter
import au.com.dius.pact.com.github.michaelbull.result.Ok
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class ProviderVerifierSpec extends Specification {

  ProviderVerifier verifier

  def setup() {
    verifier = Spy(ProviderVerifier)
  }

  def 'if no consumer filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def consumer = new ConsumerInfo('bob')

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'if a consumer filter is defined, returns false if the consumer name does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,joe' }
    def consumer = new ConsumerInfo('bob')

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    !result
  }

  def 'if a consumer filter is defined, returns true if the consumer name does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,joe,bob' }
    def consumer = new ConsumerInfo('bob')

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'trims whitespaces off the consumer names'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,\tjoe, bob\n' }
    def consumer = new ConsumerInfo('bob')

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'if no interaction filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def interaction = [:] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if an interaction filter is defined, returns false if the interaction description does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'fred' }
    def interaction = [getDescription: { 'bob' }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if an interaction filter is defined, returns true if the interaction description does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [getDescription: { 'bob' }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'uses regexs to match the description'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'bob.*' }
    def interaction = [getDescription: { 'bobby' }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if no state filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def interaction = [:] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if a state filter is defined, returns false if the interaction state does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'fred' }
    def interaction = [getProviderStates: { [new ProviderState('bob')] }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if a state filter is defined, returns true if the interaction state does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [getProviderStates: { [new ProviderState('bob')] }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if a state filter is defined, returns true if any interaction state does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [
      getProviderStates: { [new ProviderState('fred'), new ProviderState('bob')] }
    ] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'uses regexs to match the state'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob.*' }
    def interaction = [getProviderStates: { [new ProviderState('bobby')] }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if the state filter is empty, returns false if the interaction state is defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [getProviderStates: { [new ProviderState('bob')] }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if the state filter is empty, returns true if the interaction state is not defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [getProviderStates: { [] }] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if the state filter and interaction filter is defined, must match both'() {
    given:
    verifier.projectHasProperty = { true }
    verifier.projectGetProperty = {
      switch (it) {
        case ProviderVerifier.PACT_FILTER_DESCRIPTION:
          '.*ddy'
          break
        case ProviderVerifier.PACT_FILTER_PROVIDERSTATE:
          'bob.*'
          break
      }
    }
    def interaction = [
      getProviderStates: { [new ProviderState('bobby')] },
      getDescription:  { 'freddy' }
    ] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if the state filter and interaction filter is defined, is false if description does not match'() {
    given:
    verifier.projectHasProperty = { true }
    verifier.projectGetProperty = {
      switch (it) {
        case ProviderVerifier.PACT_FILTER_DESCRIPTION:
          '.*ddy'
          break
        case ProviderVerifier.PACT_FILTER_PROVIDERSTATE:
          'bob.*'
          break
      }
    }
    def interaction = [
      getProviderStates: { [new ProviderState('boddy')] },
      getDescription: { 'freddy' }
    ] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if the state filter and interaction filter is defined, is false if state does not match'() {
    given:
    verifier.projectHasProperty = { true }
    verifier.projectGetProperty = {
      switch (it) {
        case ProviderVerifier.PACT_FILTER_DESCRIPTION:
          '.*ddy'
          break
        case ProviderVerifier.PACT_FILTER_PROVIDERSTATE:
          'bob.*'
          break
      }
    }
    def interaction = [
      getProviderStates: { [new ProviderState('bobby')] },
      getDescription: { 'frebby' }
    ] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if the state filter and interaction filter is defined, is false if both do not match'() {
    given:
    verifier.projectHasProperty = { true }
    verifier.projectGetProperty = {
      switch (it) {
        case ProviderVerifier.PACT_FILTER_DESCRIPTION:
          '.*ddy'
          break
        case ProviderVerifier.PACT_FILTER_PROVIDERSTATE:
          'bob.*'
          break
      }
    }
    def interaction = [
      getProviderStates: { [new ProviderState('joe')] },
      getDescription: { 'authur' }
    ] as Interaction

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'when loading a pact file for a consumer, it should pass on any authentication options'() {
    given:
    def pactFile = new UrlSource('http://some.pact.file/')
    def consumer = new ConsumerInfo(pactSource: pactFile, pactFileAuthentication: ['basic', 'test', 'pwd'])
    verifier.pactReader = Mock(PactReader)

    when:
    verifier.loadPactFileForConsumer(consumer)

    then:
    1 * verifier.pactReader.loadPact(pactFile, ['authentication': ['basic', 'test', 'pwd']]) >> Mock(Pact)
  }

  def 'when loading a pact file for a consumer, it handles a closure'() {
    given:
    def pactFile = new UrlSource('http://some.pact.file/')
    def consumer = new ConsumerInfo(pactSource: { pactFile })
    verifier.pactReader = Mock(PactReader)

    when:
    verifier.loadPactFileForConsumer(consumer)

    then:
    1 * verifier.pactReader.loadPact(pactFile, [:]) >> Mock(Pact)
  }

  static class TestSupport {
    String testMethod() {
      '\"test method result\"'
    }
  }

  def 'is able to verify a message pact'() {
    given:
    def methods = [ TestSupport.getMethod('testMethod') ] as Set
    Message message = new Message('test', [], OptionalBody.body('\"test method result\"'.bytes))
    def interactionMessage = 'test message interaction'
    def failures = [:]
    def reporter = Mock(VerifierReporter)
    verifier.reporters = [reporter]

    when:
    def result = verifier.verifyMessagePact(methods, message, interactionMessage, failures)

    then:
    1 * reporter.bodyComparisonOk()
    1 * reporter.generatesAMessageWhich()
    1 * reporter.metadataComparisonOk()
    0 * reporter._
    result
  }

  @Unroll
  @SuppressWarnings('UnnecessaryGetter')
  def 'after verifying a pact, the results are reported back using reportVerificationResults'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    ConsumerInfo consumer = new ConsumerInfo(name: 'Test Consumer', pactSource: UnknownPactSource.INSTANCE)
    PactBrokerClient pactBrokerClient = Mock(PactBrokerClient, constructorArgs: [''])
    verifier.pactReader = Mock(PactReader)
    def statechange = Mock(StateChange) {
      executeStateChange(*_) >> new StateChangeResult(new Ok([:]))
    }
    def interaction1 = Mock(RequestResponseInteraction)
    def interaction2 = Mock(RequestResponseInteraction)
    def mockPact = Mock(Pact) {
      getSource() >> new BrokerUrlSource('http://localhost', 'http://pact-broker')
    }

    verifier.projectHasProperty = { it == ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS }
    verifier.projectGetProperty = {
      (it == ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS).toString()
    }
    verifier.stateChangeHandler = statechange

    verifier.pactReader.loadPact(_) >> mockPact
    mockPact.interactions >> [interaction1, interaction2]

    when:
    verifier.runVerificationForConsumer([:], provider, consumer, pactBrokerClient)

    then:
    1 * pactBrokerClient.publishVerificationResults(_, finalResult, '0.0.0', _)
    1 * verifier.verifyResponseFromProvider(provider, interaction1, _, _, _, _) >> result1
    1 * verifier.verifyResponseFromProvider(provider, interaction2, _, _, _, _) >> result2

    where:

    result1                 | result2                 | finalResult
    TestResult.Ok.INSTANCE  | TestResult.Ok.INSTANCE  | TestResult.Ok.INSTANCE
    TestResult.Ok.INSTANCE  | new TestResult.Failed() | new TestResult.Failed()
    new TestResult.Failed() | TestResult.Ok.INSTANCE  | new TestResult.Failed()
    new TestResult.Failed() | new TestResult.Failed() | new TestResult.Failed()
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'Do not publish verification results if not all the pact interactions have been verified'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    ConsumerInfo consumer = new ConsumerInfo(name: 'Test Consumer', pactSource: UnknownPactSource.INSTANCE)
    verifier.pactReader = Mock(PactReader)
    def statechange = Mock(StateChange) {
      executeStateChange(*_) >> new StateChangeResult(new Ok([:]))
    }
    def interaction1 = Mock(RequestResponseInteraction) {
      getDescription() >> 'Interaction 1'
    }
    def interaction2 = Mock(RequestResponseInteraction) {
      getDescription() >> 'Interaction 2'
    }
    def mockPact = Mock(Pact) {
      getSource() >> UnknownPactSource.INSTANCE
    }

    verifier.pactReader.loadPact(_) >> mockPact
    mockPact.interactions >> [interaction1, interaction2]
    verifier.verifyResponseFromProvider(provider, interaction1, _, _, _) >> true
    verifier.verifyResponseFromProvider(provider, interaction2, _, _, _) >> true

    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'Interaction 2' }
    verifier.verificationReporter = Mock(VerificationReporter)
    verifier.stateChangeHandler = statechange

    when:
    verifier.runVerificationForConsumer([:], provider, consumer)

    then:
    0 * verifier.verificationReporter.reportResults(_, _, _, _, _)
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'If the pact source is from a pact broker, publish the verification results back'() {
    given:
    def links = ['publish': 'true']
    def pact = Mock(Pact) {
      getSource() >> new BrokerUrlSource('url', 'url', links)
    }
    def client = Mock(PactBrokerClient)

    when:
    DefaultVerificationReporter.INSTANCE.reportResults(pact, TestResult.Ok.INSTANCE, '0', client, null)

    then:
    1 * client.publishVerificationResults(links, TestResult.Ok.INSTANCE, '0', null) >> new Ok(true)
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'If the pact source is not from a pact broker, ignore the verification results'() {
    given:
    def pact = Mock(Pact) {
      getSource() >> new UrlSource('url', null)
    }
    def client = Mock(PactBrokerClient)

    when:
    DefaultVerificationReporter.INSTANCE.reportResults(pact, TestResult.Ok.INSTANCE, '0', client, null)

    then:
    0 * client.publishVerificationResults(_, TestResult.Ok.INSTANCE, '0', null)
  }

  @SuppressWarnings(['UnnecessaryGetter', 'LineLength'])
  def 'Ignore the verification results if publishing is disabled'() {
    given:
    def client = Mock(PactBrokerClient)
    verifier.pactReader = Mock(PactReader)
    def statechange = Mock(StateChange)

    def source = new FileSource('test.txt' as File)
    def providerInfo = new ProviderInfo(verificationType: PactVerification.ANNOTATED_METHOD)
    def consumerInfo = new ConsumerInfo()
    consumerInfo.pactSource = source

    def interaction = new RequestResponseInteraction('Test Interaction')
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [interaction], [:], source)

    verifier.projectHasProperty = {
      it == ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS
    }
    verifier.projectGetProperty = {
      switch (it) {
        case ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS:
          return 'false'
      }
    }
    verifier.stateChangeHandler = statechange

    when:
    verifier.runVerificationForConsumer([:], providerInfo, consumerInfo, client)

    then:
    1 * verifier.pactReader.loadPact(_) >> pact
    1 * statechange.executeStateChange(_, _, _, _, _, _, _) >> new StateChangeResult(new Ok([:]), '')
    1 * verifier.verifyResponseByInvokingProviderMethods(providerInfo, consumerInfo, interaction, _, _) >> TestResult.Ok.INSTANCE
    0 * client.publishVerificationResults(_, TestResult.Ok.INSTANCE, _, _)
  }

  @Unroll
  @RestoreSystemProperties
  def 'test for pact.verifier.publishResults - #description'() {
    given:
    verifier.projectHasProperty = { value != null }
    verifier.projectGetProperty = { value }

    if (value != null) {
      System.setProperty(ProviderVerifier.PACT_VERIFIER_PUBLISH_RESULTS, value)
    }

    expect:
    verifier.publishingResultsDisabled() == result
    DefaultVerificationReporter.INSTANCE.publishingResultsDisabled() == result

    where:

    description                  | value       | result
    'Property is missing'        | null        | true
    'Property is true'           | 'true'      | false
    'Property is TRUE'           | 'TRUE'      | false
    'Property is false'          | 'false'     | true
    'Property is False'          | 'False'     | true
    'Property is something else' | 'not false' | true
  }

  @RestoreSystemProperties
  def 'defaults to system properties'() {
    given:
    System.properties['provider.verifier.test'] = 'true'

    expect:
    verifier.projectHasProperty.apply('provider.verifier.test')
    verifier.projectGetProperty.apply('provider.verifier.test') == 'true'
    !verifier.projectHasProperty.apply('provider.verifier.test.other')
    verifier.projectGetProperty.apply('provider.verifier.test.other') == null
  }

  def 'verifyInteraction returns an error result if the state change request fails'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    provider.stateChangeUrl = new URL('http://localhost:66/statechange')
    ConsumerInfo consumer = new ConsumerInfo(name: 'Test Consumer', pactSource: UnknownPactSource.INSTANCE)
    def failures = [:]
    Interaction interaction = new RequestResponseInteraction('Test Interaction',
      [new ProviderState('Test State')], new Request(), new Response(), '1234')

    when:
    def result = verifier.verifyInteraction(provider, consumer, failures, interaction)

    then:
    result instanceof TestResult.Failed
    result.results.size() == 1
    result.results[0].message == 'State change request failed'
    result.results[0].exception instanceof IOException
    result.results[0].interactionId == '1234'
  }

  def 'verifyResponseFromProvider returns an error result if the request to the provider fails with an exception'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    def failures = [:]
    Interaction interaction = new RequestResponseInteraction('Test Interaction',
      [new ProviderState('Test State')], new Request(), new Response(), '12345678')
    def client = Mock(ProviderClient)

    when:
    def result = verifier.verifyResponseFromProvider(provider, interaction, 'Test Interaction', failures, client)

    then:
    client.makeRequest(_) >> { throw new IOException('Boom!') }
    result instanceof TestResult.Failed
    result.results.size() == 1
    result.results[0].message == 'Request to provider failed with an exception'
    result.results[0].exception instanceof IOException
    result.results[0].interactionId == '12345678'
  }

  def 'verifyResponseByInvokingProviderMethods returns an error result if the method fails with an exception'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    def failures = [:]
    Interaction interaction = new Message('verifyResponseByInvokingProviderMethods Test Message', [],
      OptionalBody.empty(), new MatchingRulesImpl(), new Generators(), [:], 'abc123')
    IConsumerInfo consumer = Stub()
    def interactionMessage = 'Test'

    when:
    def result = verifier.verifyResponseByInvokingProviderMethods(provider, consumer, interaction,
      interactionMessage, failures)

    then:
    result instanceof TestResult.Failed
    result.results.size() == 1
    result.results[0].message == 'Request to provider method failed with an exception'
    result.results[0].exception instanceof Exception
    result.results[0].interactionId == 'abc123'
  }
}
