package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.ProviderState
import spock.lang.Specification

class MessageSpec extends Specification {

    def 'contentsAsBytes handles contents in string form'() {
        when:
        Message message = new Message(contents: OptionalBody.body('1 2 3 4'))

        then:
        message.contentsAsBytes() == '1 2 3 4'.bytes
    }

    def 'contentsAsBytes handles no contents'() {
        when:
        Message message = new Message(contents: OptionalBody.missing())

        then:
        message.contentsAsBytes() == []
    }

  def 'defaults to V3 provider state format when converting from a map'() {
    given:
    def map = [
      providerState: 'test state',
      providerStates: [
        [name: 'V3 state']
      ]
    ]

    when:
    Message message = Message.fromMap(map)

    then:
    message.providerState == 'V3 state'
    message.providerStates == [new ProviderState('V3 state')]
  }

  def 'falls back to V2 provider state format when converting from a map'() {
    given:
    def map = [providerState: 'test state']

    when:
    Message message = Message.fromMap(map)

    then:
    message.providerState == 'test state'
    message.providerStates == [new ProviderState('test state')]
  }

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': [:]
      ]
    ]

    when:
    def message = Message.fromMap(json)

    then:
    !message.matchingRules.empty
    message.matchingRules.hasCategory('stuff')
  }
}
