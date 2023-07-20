package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class JsonUtilsSpec extends Specification {
  def 'throws an exception if given an invalid path'() {
    when:
    JsonUtils.INSTANCE.fetchPath(null, 'jkhkjahkjhjn')

    then:
    thrown(InvalidPathExpression)
  }

  def 'fetchPath'() {
    expect:
    JsonUtils.INSTANCE.fetchPath(json ? JsonParser.parseString(json) : null, path) == value

    where:

    path     | json                               | value
    '$'      | null                               | null
    '$'      | '{}'                               | new JsonValue.Object()
    '$.a'    | '{"a": 100, "b": 200}'             | new JsonValue.Integer(100)
    '$.a[1]' | '{"a": [100, 101, 102], "b": 200}' | new JsonValue.Integer(101)
  }
}
