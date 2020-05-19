package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.json.JsonParser
import spock.lang.Specification
import spock.lang.Unroll

class JsonSpec extends Specification {

  @Unroll
  def 'object to JSON string - #desc'() {
    expect:
    Json.INSTANCE.toJson(value).serialise() == jsonString

    where:

    desc      | value                             | jsonString
    'Null'    | null                              | 'null'
    'boolean' | true                              | 'true'
    'integer' | 112                               | '112'
    'float'   | 112.66                            | '112.66'
    'string'  | 'hello'                           | '"hello"'
    'list'    | ['hello', 1, true, [a: 'A']]      | '["hello",1,true,{"a":"A"}]'
    'object'  | [hello: 'world', list: [1, 2, 3]] | '{"hello":"world","list":[1,2,3]}'
  }

  @Unroll
  def 'toBoolean - #desc'() {
    expect:
    Json.INSTANCE.toBoolean(json == null ? json : JsonParser.INSTANCE.parseString(json)) == booleanValue

    where:

    desc            | json                                    | booleanValue
    'Null'          | null                                    | false
    'Json Null'     | 'null'                                  | false
    'Boolean True'  | 'true'                                  | true
    'Boolean False' | 'false'                                 | false
    'integer'       | '112'                                   | false
    'float'         | '112.66'                                | false
    'string'        | '"hello"'                               | false
    'list'          | '["hello", 1, true, {"a": "A"}]'        | false
    'object'        | '{"hello": "world", "list": [1, 2, 3]}' | false
  }

  @Unroll
  def 'from JSON test'() {
    expect:
    Json.INSTANCE.fromJson(JsonParser.INSTANCE.parseString(json)) == value

    where:

    json                                                    | value
    'null'                                                  | null
    '100'                                                   | 100
    '100.3'                                                 | 100.3
    'true'                                                  | true
    '"a string value"'                                      | 'a string value'
    '[]'                                                    | []
    '["a string value"]'                                    | ['a string value']
    '["a string value", 2]'                                 | ['a string value', 2]
    '{}'                                                    | [:]
    '{"a": "A", "b": 1, "c": [100], "d": {"href": "blah"}}' | [a: 'A', b: 1, c: [100], d: [href: 'blah']]
  }

}
