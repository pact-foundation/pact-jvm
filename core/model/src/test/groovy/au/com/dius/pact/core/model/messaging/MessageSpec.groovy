package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.ProviderState
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class MessageSpec extends Specification {

  def 'contentsAsBytes handles contents in string form'() {
      when:
      Message message = new Message(contents: OptionalBody.body('1 2 3 4'.bytes))

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

  def 'Uses V3 provider state format when converting to a map'() {
    given:
    Message message = new Message(description: 'test', contents: OptionalBody.body('"1 2 3 4"'.bytes), providerStates: [
      new ProviderState('Test', [a: 'A', b: 100])])

    when:
    def map = message.toMap()

    then:
    map == [
      description: 'test',
      metaData: [:],
      contents: '1 2 3 4',
      providerStates: [
        [name: 'Test', params: [a: 'A', b: 100]]
      ]
    ]
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
    message1 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'.bytes))
    message2 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'.bytes))
  }

  @Ignore('Message conflicts do not work with generated values')
  def 'messages do conflict if they have the same state and description but different bodies'() {
    expect:
    message1.conflictsWith(message2)

    where:
    message1 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3'.bytes))
    message2 = new Message('description', [new ProviderState('state')], OptionalBody.body('1 2 3 4'.bytes))
  }

  @Unroll
  def 'message to map handles message content correctly'() {
    expect:
    message.toMap().contents == contents

    where:

    body                               | contentType                | contents
    '{"A": "Value A", "B": "Value B"}' | 'application/json'         | [A: 'Value A', B: 'Value B']
    '1 2 3 4'                          | 'text/plain'               | '1 2 3 4'
    new String([1, 2, 3, 4] as byte[]) | 'application/octet-stream' | 'AQIDBA=='

    message = new Message(contents: OptionalBody.body(body.bytes), metaData: [contentType: contentType])
  }

}
