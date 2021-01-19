package au.com.dius.pact.core.model.generators

import au.com.dius.pact.core.support.Json
import spock.lang.Specification

class JsonContentTypeHandlerSpec extends Specification {

  def 'applies the generator to a map entry'() {
    given:
    def map = [a: 'A', b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(map), null, null)
    def key = '$.b'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [a: 'A', b: 'X', c: 'C']
  }

  def 'does not apply the generator when field is not in map'() {
    given:
    def map = [a: 'A', b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(map), null, null)
    def key = '$.d'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [a: 'A', b: 'B', c: 'C']
  }

  def 'does not apply the generator when not a map'() {
    given:
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(100), null, null)
    def key = '$.d'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    body.value.asNumber() == 100
  }

  def 'applies the generator to a list item'() {
    given:
    def list = ['A', 'B', 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(list), null, null)
    def key = '$[1]'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toList(body.value) == ['A', 'X', 'C']
  }

  def 'does not apply the generator if the index is not in the list'() {
    given:
    def list = ['A', 'B', 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(list), null, null)
    def key = '$[3]'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toList(body.value) == ['A', 'B', 'C']
  }

  def 'does not apply the generator when not a list'() {
    given:
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(100), null, null)
    def key = '$[3]'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    body.value.asNumber() == 100
  }

  def 'applies the generator to the root'() {
    given:
    def bodyValue = 100
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(bodyValue), null, null)
    def key = '$'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    body.value.asString() == 'X'
  }

  def 'applies the generator to the object graph'() {
    given:
    def graph = [a: ['A', [a: 'A', b: ['1': '1', '2': '2'], c: 'C'], 'C'], b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(graph), null, null)
    def key = '$.a[1].b[\'2\']'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [a: ['A', [a: 'A', b: ['1': '1', '2': 'X'], c: 'C'], 'C'], b: 'B', c: 'C']
  }

  def 'does not apply the generator to the object graph when the expression does not match'() {
    given:
    def graph = [d: 'A', b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(graph), null, null)
    def key = '$.a[1].b[\'2\']'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [d: 'A', b: 'B', c: 'C']
  }

  def 'applies the generator to all map entries'() {
    given:
    def map = [a: 'A', b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(map), null, null)
    def key = '$.*'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [a: 'X', b: 'X', c: 'X']
  }

  def 'applies the generator to all list items'() {
    given:
    def list = ['A', 'B', 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(list), null, null)
    def key = '$[*]'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toList(body.value) == ['X', 'X', 'X']
  }

  def 'applies the generator to the object graph with wildcard'() {
    given:
    def graph = [a: ['A', [a: 'A', b: ['1', '2'], c: 'C'], 'C'], b: 'B', c: 'C']
    QueryResult body = new QueryResult(Json.INSTANCE.toJson(graph), null, null)
    def key = '$.*[1].b[*]'
    def generator = Mock(Generator) {
      generate(_, _) >> 'X'
    }

    when:
    JsonContentTypeHandler.INSTANCE.applyKey(body, key, generator, [:])

    then:
    Json.INSTANCE.toMap(body.value) == [a: ['A', [a: 'A', b: ['X', 'X'], c: 'C'], 'C'], b: 'B', c: 'C']
  }
}
