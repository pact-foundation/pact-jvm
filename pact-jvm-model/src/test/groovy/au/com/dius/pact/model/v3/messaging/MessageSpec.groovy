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
        'stuff': ['': [matchers: [ [match: 'type'] ] ] ]
      ]
    ]

    when:
    def message = Message.fromMap(json)

    then:
    !message.matchingRules.empty
    message.matchingRules.hasCategory('stuff')
  }

  def 'unique key test'() {
    expect:
    interaction1.uniqueKey() == interaction1.uniqueKey()
    interaction1.uniqueKey() == interaction2.uniqueKey()
    interaction1.uniqueKey() != interaction3.uniqueKey()
    interaction1.uniqueKey() != interaction4.uniqueKey()
    interaction1.uniqueKey() != interaction5.uniqueKey()
    interaction3.uniqueKey() != interaction4.uniqueKey()
    interaction3.uniqueKey() != interaction5.uniqueKey()
    interaction4.uniqueKey() != interaction5.uniqueKey()

    where:
    interaction1 = new Message('description 1+2')
    interaction2 = new Message('description 1+2')
    interaction3 = new Message('description 1+2', [new ProviderState('state 3')])
    interaction4 = new Message('description 4')
    interaction5 = new Message('description 4', [new ProviderState('state 5')])
  }

  def 'messages do not conflict if they have different states'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', [new ProviderState('state')])
    message2 = new Message('description', [new ProviderState('state 2')])
  }

  def 'messages do not conflict if they have different descriptions'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', [new ProviderState('state')])
    message2 = new Message('description 2', [new ProviderState('state')])
  }

  def 'messages do not conflict if they are identical'() {
    expect:
    !message1.conflictsWith(message2)

    where:
    message1 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'))
    message2 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'))
  }

  def 'messages do not conflict if they have the same state and description but different bodies'() {
    expect:
    message1.conflictsWith(message2)

    where:
    message1 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'))
    message2 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3 4'))
  }

}
