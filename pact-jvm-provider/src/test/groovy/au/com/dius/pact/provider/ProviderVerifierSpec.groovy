package au.com.dius.pact.provider

import au.com.dius.pact.model.BrokerUrlSource
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.UnknownPactSource
import au.com.dius.pact.model.UrlSource
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.provider.broker.PactBrokerClient
import au.com.dius.pact.provider.reporters.VerifierReporter
import com.github.kittinunf.result.Result
import spock.lang.Specification
import spock.lang.Unroll

class ProviderVerifierSpec extends Specification {

  ProviderVerifier verifier

  def setup() {
    verifier = Spy(ProviderVerifier)
  }

  def 'if no consumer filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def consumer = [:]

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'if a consumer filter is defined, returns false if the consumer name does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,joe' }
    def consumer = [name: 'bob']

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    !result
  }

  def 'if a consumer filter is defined, returns true if the consumer name does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,joe,bob' }
    def consumer = [name: 'bob']

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'trims whitespaces off the consumer names'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_CONSUMERS }
    verifier.projectGetProperty = { 'fred,\tjoe, bob\n' }
    def consumer = [name: 'bob']

    when:
    boolean result = verifier.filterConsumers(consumer)

    then:
    result
  }

  def 'if no interaction filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def interaction = [:]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if an interaction filter is defined, returns false if the interaction description does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'fred' }
    def interaction = [description: 'bob']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if an interaction filter is defined, returns true if the interaction description does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [description: 'bob']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'uses regexs to match the description'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'bob.*' }
    def interaction = [description: 'bobby']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if no state filter is defined, returns true'() {
    given:
    verifier.projectHasProperty = { false }
    def interaction = [:]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if a state filter is defined, returns false if the interaction state does not match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'fred' }
    def interaction = [providerStates: [new ProviderState('bob')]]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if a state filter is defined, returns true if the interaction state does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [providerStates: [new ProviderState('bob')]]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if a state filter is defined, returns true if any interaction state does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [providerStates: [new ProviderState('fred'), new ProviderState('bob')]]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'uses regexs to match the state'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob.*' }
    def interaction = [providerStates: [new ProviderState('bobby')]]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if the state filter is empty, returns false if the interaction state is defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [providerStates: [new ProviderState('bob')]]

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if the state filter is empty, returns true if the interaction state is not defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [providerStates: []]

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
    def interaction = [providerStates: [new ProviderState('bobby')], description: 'freddy']

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
    def interaction = [providerStates: [new ProviderState('boddy')], description: 'freddy']

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
    def interaction = [providerStates: [new ProviderState('bobby')], description: 'frebby']

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
    def interaction = [providerStates: [new ProviderState('joe')], description: 'authur']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'when loading a pact file for a consumer, it should pass on any authentication options'() {
    given:
    def pactFile = new UrlSource('http://some.pact.file/')
    def consumer = new ConsumerInfo(pactSource: pactFile, pactFileAuthentication: ['basic', 'test', 'pwd'])
    GroovyMock(PactReader, global: true)

    when:
    verifier.loadPactFileForConsumer(consumer)

    then:
    1 * PactReader.loadPact(['authentication': ['basic', 'test', 'pwd']], pactFile) >> Mock(Pact)
  }

  def 'when loading a pact file for a consumer, it handles a closure'() {
    given:
    def pactFile = new UrlSource('http://some.pact.file/')
    def consumer = new ConsumerInfo(pactSource: { pactFile })
    GroovyMock(PactReader, global: true)

    when:
    verifier.loadPactFileForConsumer(consumer)

    then:
    1 * PactReader.loadPact([:], pactFile) >> Mock(Pact)
  }

  class TestSupport {
    String testMethod() {
      '\"test method result\"'
    }
  }

  def 'is able to verify a message pact'() {
    given:
    def methods = [ TestSupport.getMethod('testMethod') ] as Set
    Message message = new Message(contents: OptionalBody.body('\"test method result\"'))
    def interactionMessage = 'test message interaction'
    def failures = [:]
    def reporter = Mock(VerifierReporter)
    verifier.reporters << reporter

    when:
    def result = verifier.verifyMessagePact(methods, message, interactionMessage, failures)

    then:
    1 * reporter.bodyComparisonOk()
    1 * reporter.generatesAMessageWhich()
    0 * reporter._
    result
  }

  @Unroll
  @SuppressWarnings('UnnecessaryGetter')
  def 'after verifying a pact, the results are reported back using reportVerificationResults'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    ConsumerInfo consumer = new ConsumerInfo(name: 'Test Consumer', pactSource: UnknownPactSource.INSTANCE)
    GroovyMock(PactReader, global: true)
    GroovyMock(ProviderVerifierKt, global: true)
    GroovyMock(StateChange, global: true)
    def interaction1 = Mock(Interaction)
    def interaction2 = Mock(Interaction)
    def mockPact = Mock(Pact) {
      getSource() >> UnknownPactSource.INSTANCE
    }

    PactReader.loadPact(_) >> mockPact
    mockPact.interactions >> [interaction1, interaction2]
    StateChange.executeStateChange(*_) >> new StateChange.StateChangeResult(true)

    when:
    verifier.runVerificationForConsumer([:], provider, consumer)

    then:
    1 * ProviderVerifierKt.reportVerificationResults(_, finalResult, '0.0.0')
    1 * verifier.verifyResponseFromProvider(provider, interaction1, _, _) >> result1
    1 * verifier.verifyResponseFromProvider(provider, interaction2, _, _) >> result2

    where:

    result1 | result2 | finalResult
    true    | true    | true
    true    | false   | false
    false   | true    | false
    false   | false   | false
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'Do not publish verification results if the pact interactions have been filtered'() {
    given:
    ProviderInfo provider = new ProviderInfo('Test Provider')
    ConsumerInfo consumer = new ConsumerInfo(name: 'Test Consumer', pactSource: UnknownPactSource.INSTANCE)
    GroovyMock(PactReader, global: true)
    GroovyMock(ProviderVerifierKt, global: true)
    GroovyMock(StateChange, global: true)
    def interaction1 = Mock(Interaction) {
      getDescription() >> 'Interaction 1'
    }
    def interaction2 = Mock(Interaction) {
      getDescription() >> 'Interaction 2'
    }
    def mockPact = Mock(Pact) {
      getSource() >> UnknownPactSource.INSTANCE
    }

    PactReader.loadPact(_) >> mockPact
    mockPact.interactions >> [interaction1, interaction2]
    StateChange.executeStateChange(*_) >> new StateChange.StateChangeResult(true)
    verifier.verifyResponseFromProvider(provider, interaction1, _, _) >> true
    verifier.verifyResponseFromProvider(provider, interaction2, _, _) >> true

    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_DESCRIPTION }
    verifier.projectGetProperty = { 'Interaction 2' }

    when:
    verifier.runVerificationForConsumer([:], provider, consumer)

    then:
    0 * ProviderVerifierKt.reportVerificationResults(_, _, _)
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
    ProviderVerifierKt.reportVerificationResults(pact, true, '0', client)

    then:
    1 * client.publishVerificationResults(links, true, '0', null) >> new Result.Success(true)
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'If the pact source is not from a pact broker, ignore the verification results'() {
    given:
    def pact = Mock(Pact) {
      getSource() >> new UrlSource('url', null)
    }
    def client = Mock(PactBrokerClient)

    when:
    ProviderVerifierKt.reportVerificationResults(pact, true, '0', client)

    then:
    0 * client.publishVerificationResults(_, true, '0', null)
  }
}
