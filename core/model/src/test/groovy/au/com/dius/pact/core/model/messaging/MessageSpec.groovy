package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import spock.lang.Specification
import spock.lang.Unroll

class MessageSpec extends Specification {

  def 'contentsAsBytes handles contents in string form'() {
      when:
      Message message = new Message('test', [], OptionalBody.body('1 2 3 4'.bytes))

      then:
      message.contentsAsBytes() == '1 2 3 4'.bytes
  }

  def 'contentsAsBytes handles no contents'() {
      when:
      Message message = new Message('test', [], OptionalBody.missing())

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
    message.providerStates == [new ProviderState('V3 state')]
  }

  def 'falls back to V2 provider state format when converting from a map'() {
    given:
    def map = [providerState: 'test state']

    when:
    Message message = Message.fromMap(map)

    then:
    message.providerStates == [new ProviderState('test state')]
  }

  def 'Uses V3 provider state format when converting to a map'() {
    given:
    Message message = new Message('test', [new ProviderState('Test', [a: 'A', b: 100])],
      OptionalBody.body('"1 2 3 4"'.bytes))

    when:
    def map = message.toMap(PactSpecVersion.V3)

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

  @Unroll
  def 'get content type test'() {
    expect:
    message.contentType == result

    where:

    key            | contentType                | result
    'contentType'  | 'application/json'         | 'application/json'
    'Content-Type' | 'text/plain'               | 'text/plain'
    'contenttype'  | 'application/octet-stream' | 'application/octet-stream'
    'none'         | 'none'                     | 'application/json'

    message = new Message(metaData: [(key): contentType])
  }

  @Unroll
  def 'format contents should handle content types correctly - #contentType'() {
    expect:
    message.formatContents() == result

    where:

    contentType                                | result
    'application/json'                         | [a: 100.0, b: 'test']
    'application/json;charset=UTF-8'           | [a: 100.0, b: 'test']
    'application/json; charset\u003dUTF-8'     | [a: 100.0, b: 'test']
    'application/hal+json; charset\u003dUTF-8' | [a: 100.0, b: 'test']
    'text/plain'                               | '{"a": 100.0, "b": "test"}'
    'application/octet-stream;charset=UTF-8'   | 'eyJhIjogMTAwLjAsICJiIjogInRlc3QifQ=='
    'application/octet-stream'                 | 'eyJhIjogMTAwLjAsICJiIjogInRlc3QifQ=='
    ''                                         | '{"a": 100.0, "b": "test"}'
    null                                       | [a: 100.0, b: 'test']

    message = new Message('test', [], OptionalBody.body('{"a": 100.0, "b": "test"}'.bytes),
      new MatchingRulesImpl(), new Generators(), ['contentType': contentType])
  }

}
