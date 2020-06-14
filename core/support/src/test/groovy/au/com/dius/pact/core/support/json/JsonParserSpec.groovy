package au.com.dius.pact.core.support.json

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class JsonParserSpec extends Specification {

  @Unroll
  def 'invalid document - #description'() {
    when:
    JsonParser.INSTANCE.parseString(json)

    then:
    thrown(JsonException)

    where:

    description                      | json
    'empty document'                 | ''
    'whitespace'                     | '  \t\n\r'
    'minus with no following digits' | '  -'
    'invalid case'                   | 'Null'
    'invalid value after other'      | 'null true'
    'unterminated string'            | '"null true'
    'unterminated array'             | '["null", true'
    'unterminated object'            | '{"null": true'
    'invalid end array'              | '12]'
    'invalid end object'             | 'true}'
    'invalid comma'                  | '1234,'
    'invalid object key'             | '{null: true}'
    'unterminated object key'        | '{"null: true}'
    'invalid object key'             | '{"nu\\ll": true}'
    'missing colon'                  | '{"null" true}'
    'missing comma in array'         | '["null" true]'
    'missing comma in object'        | '{"null": true "other": false}'
  }

  @Unroll
  def 'valid document - #description'() {
    when:
    def value = JsonParser.INSTANCE.parseString(json)

    then:
    value == result

    where:

    description    | json                     | result
    'integer'      | '  1234'                 | new JsonValue.Integer('1234'.chars)
    'decimal'      | '  1234.56 '             | new JsonValue.Decimal('1234.56'.chars)
    'true'         | 'true'                   | JsonValue.True.INSTANCE
    'false'        | 'false'                  | JsonValue.False.INSTANCE
    'null'         | 'null'                   | JsonValue.Null.INSTANCE
    'string'       | '"null"'                 | new JsonValue.StringValue('null'.chars)
    'array'        | '[1, 200, 3, "4"]'       | new JsonValue.Array([new JsonValue.Integer('1'.chars), new JsonValue.Integer('200'.chars), new JsonValue.Integer('3'.chars), new JsonValue.StringValue('4'.chars)])
    '2d array'     | '[[1, 2], 3, "4"]'       | new JsonValue.Array([new JsonValue.Array([new JsonValue.Integer('1'.chars), new JsonValue.Integer('2'.chars)]), new JsonValue.Integer('3'.chars), new JsonValue.StringValue('4'.chars)])
    'object'       | '{"1": 200, "3": "4"}'   | new JsonValue.Object(['1': new JsonValue.Integer('200'.chars), '3': new JsonValue.StringValue('4'.chars)])
    '2d object'    | '{"1": 2, "3": {"4":5}}' | new JsonValue.Object(['1': new JsonValue.Integer('2'.chars), '3': new JsonValue.Object(['4': new JsonValue.Integer('5'.chars)])])
    'empty object' | '{}'                     | new JsonValue.Object([:])
    'empty array'  | '[]'                     | new JsonValue.Array([])
    'empty string' | '""'                     | new JsonValue.StringValue(''.chars)
  }

  def 'can parse a pact file'() {
    given:
    def pactfile = JsonParserSpec.getResourceAsStream('/v3-pact-broker.json')

    when:
    def value = JsonParser.INSTANCE.parseStream(pactfile)

    then:
    value instanceof JsonValue.Object
    value.entries.keySet() == ['consumer', 'provider', 'interactions', 'metadata', 'createdAt', '_links'] as Set
    value.entries['consumer'] == new JsonValue.Object(['name': new JsonValue.StringValue('Foo Web Client'.chars)])
    value.entries['provider'] == new JsonValue.Object(['name': new JsonValue.StringValue('Activity Service'.chars)])
    value.entries['metadata'] == new JsonValue.Object(['pactSpecification': new JsonValue.Object(['version': new JsonValue.StringValue('3.0.0'.chars)])])
  }
}
