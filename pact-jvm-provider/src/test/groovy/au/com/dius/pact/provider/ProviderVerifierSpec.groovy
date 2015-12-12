package au.com.dius.pact.provider

import au.com.dius.pact.model.Pact
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import au.com.dius.pact.model.v3.messaging.Message
import au.com.dius.pact.model.v3.messaging.MessagePact
import spock.lang.Specification

class ProviderVerifierSpec extends Specification {

  ProviderVerifier verifier

  def setup() {
    verifier = new ProviderVerifier()
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
    def interaction = [providerState: 'bob']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if a state filter is defined, returns true if the interaction state does match'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob' }
    def interaction = [providerState: 'bob']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'uses regexs to match the state'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { 'bob.*' }
    def interaction = [providerState: 'bobby']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    result
  }

  def 'if the state filter is empty, returns false if the interaction state is defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [providerState: 'bob']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'if the state filter is empty, returns true if the interaction state is not defined'() {
    given:
    verifier.projectHasProperty = { it == ProviderVerifier.PACT_FILTER_PROVIDERSTATE }
    verifier.projectGetProperty = { '' }
    def interaction = [providerState: null]

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
    def interaction = [providerState: 'bobby', description: 'freddy']

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
    def interaction = [providerState: 'boddy', description: 'freddy']

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
    def interaction = [providerState: 'bobby', description: 'frebby']

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
    def interaction = [providerState: 'joe', description: 'authur']

    when:
    boolean result = verifier.filterInteractions(interaction)

    then:
    !result
  }

  def 'extract interactions from a V2 pact'() {
    given:
    def interaction = new RequestResponseInteraction('test interaction')
    def pact = new RequestResponsePact(null, null, [interaction])

    when:
    def result = verifier.interactions(pact)

    then:
    result == [interaction]
  }

  def 'extract interactions from a Message pact'() {
    given:
    def interaction = new Message('test message')
    def pact = new MessagePact(messages: [ interaction ])

    when:
    def result = verifier.interactions(pact)

    then:
    result == [interaction]
  }

  def 'when loading a pact file for a consumer, it should pass on any authentication options'() {
    given:
    def pactFile = new URL('http://some.pact.file/')
    def consumer = new ConsumerInfo(pactFile: pactFile, pactFileAuthentication: ['basic', 'test', 'pwd'])
    GroovyMock(PactReader, global: true)

    when:
    verifier.loadPactFileForConsumer(consumer)

    then:
    1 * PactReader.loadPact(['authentication': ['basic', 'test', 'pwd']], pactFile) >> Mock(Pact)
  }
}
