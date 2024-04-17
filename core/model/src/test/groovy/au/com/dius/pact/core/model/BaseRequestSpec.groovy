package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class BaseRequestSpec extends Specification {
  def 'parseQueryParametersToMap'() {
    expect:
    BaseRequest.parseQueryParametersToMap(json) == value

    where:

    json                                 | value
    null                                 | [:]
    JsonValue.Null.INSTANCE              | [:]
    JsonValue.True.INSTANCE              | [:]
    JsonValue.False.INSTANCE             | [:]
    new JsonValue.Integer(100)           | [:]
    new JsonValue.Decimal(100.0)         | [:]
    new JsonValue.Array([])              | [:]
    new JsonValue.StringValue('a=1&b=2') | [a: ['1'], b: ['2']]
  }

  def 'parseQueryParametersToMap - with a JSON map'() {
    expect:
    BaseRequest.parseQueryParametersToMap(JsonParser.parseString(json).asObject()) == value

    where:

    json                | value
    '{}'                | [:]
    '{"a": "1"}'        | [a: ['1']]
    '{"a": ["1"]}'      | [a: ['1']]
    '{"a": ["", ""]}'   | [a: ['', '']]
    '{"a": [null, ""]}' | [a: [null, '']]
  }
}
