package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

@SuppressWarnings('LineLength')
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

  def 'resolve path - with root'() {
    expect:
    JsonUtils.INSTANCE.resolvePath(JsonValue.Null.INSTANCE, DocPath.root()) == []
    JsonUtils.INSTANCE.resolvePath(JsonValue.True.INSTANCE, DocPath.root()) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[1, 2, 3]'), DocPath.root()) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('{"a":1}'), DocPath.root()) == []
  }

  def 'resolve path - with field'() {
    given:
    def path = new DocPath('$.a')

    expect:
    JsonUtils.INSTANCE.resolvePath(JsonValue.Null.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonValue.True.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('{"a": 100, "b": 200}'), path) == ['/a']
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200}]'), path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('{"a": {"x": {"z": 400}, "y": 300}, "b": 200}'), new DocPath('$.a.x')) == ['/a/x']
  }

  def 'resolve path - with index'() {
    given:
    def path = new DocPath('$[0]')

    expect:
    JsonUtils.INSTANCE.resolvePath(JsonValue.Null.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonValue.True.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('{"a": 100, "b": 200}'), path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200}]'), path) == ['/0']
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200}]'), new DocPath('$[0].b')) == ['/0/b']
  }

  def 'resolve path - with star'() {
    given:
    def path = new DocPath('$.*')

    expect:
    JsonUtils.INSTANCE.resolvePath(JsonValue.Null.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonValue.True.INSTANCE, path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('{"a": 100, "b": 200}'), path) == ['/a', '/b']
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200},{"a": 100, "b": 200}]'), path) == []
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200},{"a": 100, "b": 200}]'), new DocPath('$[*]')) == ['/0', '/1']
    JsonUtils.INSTANCE.resolvePath(JsonParser.parseString('[{"a": 100, "b": 200},{"a": 100, "b": 200}]'), new DocPath('$[*].b')) == ['/0/b', '/1/b']
  }
}
