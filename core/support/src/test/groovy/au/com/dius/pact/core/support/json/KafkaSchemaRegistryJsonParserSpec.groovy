package au.com.dius.pact.core.support.json

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class KafkaSchemaRegistryJsonParserSpec extends Specification {

  @Unroll
  def 'invalid document - #description'() {
    when:
    KafkaSchemaRegistryJsonParser.INSTANCE.parseString(json)

    then:
    thrown(JsonException)

    where:

    description                        | json
    'empty document'                   | ''
    'empty document after magic bytes' | '     '
    'whitespace'                       | '  \t\n\r'
    'whitespace after magic bytes'     | '       \t\n\r'
    'minus with no following digits'   | '  -'
    'invalid case'                     | 'Null'
    'invalid value after other'        | '     null true'
    'unterminated string'              | '     "null true'
    'unterminated array'               | '["null", true'
    'unterminated object'              | '{"null": true'
    'invalid end array'                | '12]'
    'invalid end object'               | 'true}'
    'invalid comma'                    | '1234,'
    'invalid object key'               | '{null: true}'
    'unterminated object key'          | '{"null: true}'
    'invalid object key'               | '{"nu\\ll": true}'
    'missing colon'                    | '{"null" true}'
    'missing comma in array'           | '["null" true]'
    'missing comma in object'          | '{"null": true "other": false}'
  }

  @Unroll
  def 'valid document - #description'() {
    when:
    def value = KafkaSchemaRegistryJsonParser.INSTANCE.parseString(json)

    then:
    value == result

    where:

    description                       | json                     | result
    'integer'                         | '       1234'                 | new JsonValue.Integer('1234'.chars)
    'decimal'                         | '       1234.56 '             | new JsonValue.Decimal('1234.56'.chars)
    'true'                            | '     true'                   | JsonValue.True.INSTANCE
    'false'                           | '     false'                  | JsonValue.False.INSTANCE
    'null'                            | '     null'                   | JsonValue.Null.INSTANCE
    'string'                          | '     "null"'                 | new JsonValue.StringValue('null'.chars)
    'array'                           | '     [1, 200, 3, "4"]'       | new JsonValue.Array([new JsonValue.Integer('1'.chars), new JsonValue.Integer('200'.chars), new JsonValue.Integer('3'.chars), new JsonValue.StringValue('4'.chars)])
    '2d array'                        | '     [[1, 2], 3, "4"]'       | new JsonValue.Array([new JsonValue.Array([new JsonValue.Integer('1'.chars), new JsonValue.Integer('2'.chars)]), new JsonValue.Integer('3'.chars), new JsonValue.StringValue('4'.chars)])
    'object'                          | '     {"1": 200, "3": "4"}'   | new JsonValue.Object(['1': new JsonValue.Integer('200'.chars), '3': new JsonValue.StringValue('4'.chars)])
    'object with decimal value 1'     | '     {"1": 20.25}'           | new JsonValue.Object(['1': new JsonValue.Decimal('20.25'.chars)])
    'object with decimal value 2'     | '     {"1": 200.25}'          | new JsonValue.Object(['1': new JsonValue.Decimal('200.25'.chars)])
    '2d object'                       | '     {"1": 2, "3": {"4":5}}' | new JsonValue.Object(['1': new JsonValue.Integer('2'.chars), '3': new JsonValue.Object(['4': new JsonValue.Integer('5'.chars)])])
    'empty object'                    | '     {}'                     | new JsonValue.Object([:])
    'empty array'                     | '     []'                     | new JsonValue.Array([])
    'empty string'                    | '     ""'                     | new JsonValue.StringValue(''.chars)
    'keys with special chars'         | '     {"ä": "äbc"}'           | new JsonValue.Object(['ä': new JsonValue.StringValue('äbc'.chars)])
  }

  @SuppressWarnings('TrailingWhitespace')
  def 'parse a basic message pact'() {
    given:
    def pact = '''     {
      "consumer": {
        "name": "consumer"
      },
      "provider": {
        "name": "provider"
      },
      "messages": [
        {
          "metaData": {
            "contentType": "application/json"
          },
          "providerStates": [
            {
              "name": "message exists",
              "params": {}
            }
          ],
          "contents": "Hello",
          "matchingRules": {
           
          },
          "description": "a hello message"
        }
      ],
      "metadata": {
        "pactSpecification": {
          "version": "3.0.0"
        },
        "pact-jvm": {
          "version": "4.0.10"
        }
      }
    }
    '''

    when:
    def value = KafkaSchemaRegistryJsonParser.INSTANCE.parseString(pact)

    then:
    value instanceof JsonValue.Object
    value.entries.keySet() == ['consumer', 'provider', 'messages', 'metadata'] as Set
    value.entries['consumer'] == new JsonValue.Object(['name': new JsonValue.StringValue('consumer'.chars)])
    value.entries['provider'] == new JsonValue.Object(['name': new JsonValue.StringValue('provider'.chars)])
    value.entries['metadata'] == new JsonValue.Object([
      'pactSpecification': new JsonValue.Object(['version': new JsonValue.StringValue('3.0.0'.chars)]),
      'pact-jvm': new JsonValue.Object(['version': new JsonValue.StringValue('4.0.10'.chars)])
    ])
  }
}
