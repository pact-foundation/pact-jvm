package au.com.dius.pact.core.model.messaging

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Json
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
    Message message = Message.fromJson(Json.INSTANCE.toJson(map).asObject())

    then:
    message.providerStates == [new ProviderState('V3 state')]
  }

  def 'falls back to V2 provider state format when converting from a map'() {
    given:
    def map = [providerState: 'test state']

    when:
    Message message = Message.fromJson(Json.INSTANCE.toJson(map).asObject())

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
      contents: '"1 2 3 4"',
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
    def message = Message.fromJson(Json.INSTANCE.toJson(json).asObject())

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
    message.toMap(PactSpecVersion.V3).contents == contents

    where:

    body                                     | contentType                              | contents
    '{"A": "Value A", "B": "Value B"}'       | 'application/json'                       | [A: 'Value A', B: 'Value B']
    '{"A": "Value A", "B": "Value B"}'       | ''                                       | [A: 'Value A', B: 'Value B']
    '1 2 3 4'                                | 'text/plain'                             | '1 2 3 4'
    '1 2 3 4'                                | ''                                       | '1 2 3 4'
    new String([1, 2, 3, 4] as byte[])       | 'application/octet-stream'               | 'AQIDBA=='
    kStr('{"A": "Value A", "B": "Value B"}') | 'application/vnd.schemaregistry.v1+json' | [A: 'Value A', B: 'Value B']
    kStr('{"A": "Value A", "B": "Value B"}') | ''                                       | [A: 'Value A', B: 'Value B']
    kStr('invalid json')                     | 'application/vnd.schemaregistry.v1+json' | kStr('invalid json')

    message = new Message('test', [], OptionalBody.body(body.bytes, new ContentType(contentType)),
      new MatchingRulesImpl(), new Generators(), [contentType: contentType])
  }

  @Unroll
  def 'message to map handles message content correctly - with only metadata'() {
    expect:
    message.toMap(PactSpecVersion.V3).contents == contents

    where:

    body                               | contentType                | contents
    '{"A": "Value A", "B": "Value B"}' | 'application/json'         | [A: 'Value A', B: 'Value B']
    '{"A": "Value A", "B": "Value B"}' | ''                         | [A: 'Value A', B: 'Value B']
    '{"A": "Value A", "B": "Value B"}' | 'text/plain'               | '{"A": "Value A", "B": "Value B"}'
    '1 2 3 4'                          | 'text/plain'               | '1 2 3 4'
    '1 2 3 4'                          | ''                         | '1 2 3 4'
    new String([1, 2, 3, 4] as byte[]) | 'application/octet-stream' | 'AQIDBA=='

    message = new Message('test', [], OptionalBody.body(body.bytes),
      new MatchingRulesImpl(), new Generators(), [contentType: contentType])
  }

  @Unroll
  def 'message to map handles message content correctly - with no metadata'() {
    expect:
    message.toMap(PactSpecVersion.V3).contents == contents

    where:

    body                               | contentType                | contents
    '{"A": "Value A", "B": "Value B"}' | 'application/json'         | [A: 'Value A', B: 'Value B']
    '{"A": "Value A", "B": "Value B"}' | ''                         | [A: 'Value A', B: 'Value B']
    '{"A": "Value A", "B": "Value B"}' | 'text/plain'               | '{"A": "Value A", "B": "Value B"}'
    '1 2 3 4'                          | 'text/plain'               | '1 2 3 4'
    '1 2 3 4'                          | ''                         | '1 2 3 4'
    new String([1, 2, 3, 4] as byte[]) | 'application/octet-stream' | 'AQIDBA=='

    message = new Message('test', [], OptionalBody.body(body.bytes, new ContentType(contentType)),
      new MatchingRulesImpl(), new Generators(), [:])
  }

  @Unroll
  def 'get content type test'() {
    expect:
    message.contentType.toString() == result

    where:

    key            | contentType                | result
    'contentType'  | 'application/json'         | 'application/json'
    'Content-Type' | 'text/plain'               | 'text/plain'
    'contenttype'  | 'application/octet-stream' | 'application/octet-stream'
    'none'         | 'none'                     | 'null'

    message = new Message('Test').withMetaData([(key): contentType])
  }

  @Unroll
  def 'format contents should handle content types correctly - #contentType'() {
    expect:
    message.formatContents() == result

    where:

    contentType                                | result
    'application/json'                         | '{\n  "a": 100.0,\n  "b": "test"\n}'
    'application/json;charset=UTF-8'           | '{\n  "a": 100.0,\n  "b": "test"\n}'
    'application/json; charset\u003dUTF-8'     | '{\n  "a": 100.0,\n  "b": "test"\n}'
    'application/hal+json; charset\u003dUTF-8' | '{\n  "a": 100.0,\n  "b": "test"\n}'
    'text/plain'                               | '{"a": 100.0, "b": "test"}'
    'application/octet-stream;charset=UTF-8'   | 'eyJhIjogMTAwLjAsICJiIjogInRlc3QifQ=='
    'application/octet-stream'                 | 'eyJhIjogMTAwLjAsICJiIjogInRlc3QifQ=='
    ''                                         | '{\n  "a": 100.0,\n  "b": "test"\n}'
    null                                       | '{\n  "a": 100.0,\n  "b": "test"\n}'

    message = new Message('test', [], OptionalBody.body('{"a": 100.0, "b": "test"}'.bytes,
      ContentType.fromString(contentType)),
      new MatchingRulesImpl(), new Generators(), ['contentType': contentType])
  }

  def 'kafka schema registry content type should be handled - #contentType'() {
    expect:
    message.formatContents() == result

    where:

    contentType                                | result
    'application/vnd.schemaregistry.v1+json'   | '{\n  "a": 100.0,\n  "b": "test"\n}'

    message = new Message('test',
            [],
            OptionalBody.body(createKafkaSchemaRegistryCompliantBytes('{"a": 100.0, "b": "test"}'),
                    ContentType.fromString(contentType)),
            new MatchingRulesImpl(), new Generators(), ['contentType': contentType])
  }

  private byte[] createKafkaSchemaRegistryCompliantBytes(String json) {
    ((kafkaSchemaRegistryMagicBytes() as List) << (json.bytes as List)).flatten()
  }

  private String kStr(String json) {
    new String(kafkaSchemaRegistryMagicBytes()) + json
  }

  private byte[] kafkaSchemaRegistryMagicBytes() {
    def zero = (byte) 0x00
    def one = (byte) 0x01
    new byte[] {zero, zero, zero, zero, one}
  }

  def 'when upgrading message to V4, rename the matching rules from body to content'() {
    given:
    MatchingRules matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$', TypeMatcher.INSTANCE)
    def message = new Message('description', [], OptionalBody.missing(), matchingRules)

    when:
    def v4Message = message.asV4Interaction()

    then:
    v4Message.toMap(PactSpecVersion.V4).matchingRules == [content: ['$': [matchers: [[match: 'type']], combine: 'AND']]]
  }
}
