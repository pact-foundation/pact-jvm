package au.com.dius.pact.core.support

import spock.lang.Specification
import spock.lang.Unroll

class JsonSpec extends Specification {

  @Unroll
  def 'object to JSON string - #desc'() {
    expect:
    Json.INSTANCE.toJson(value).toString() == jsonString

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

}
