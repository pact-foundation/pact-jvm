package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification
import spock.lang.Unroll

class DslPartSpec extends Specification {

  @SuppressWarnings(['MethodCount', 'FieldName'])
  private static final DslPart subject = new DslPart('', '') {

    @Override
    void putObjectPrivate(DslPart object) { }

    @Override
    void putArrayPrivate(DslPart object) { }

    JsonValue.Object body = null

    @Override
    PactDslJsonArray array(String name) { null }

    @Override
    PactDslJsonArray array() { null }

    @Override
    DslPart closeArray() { null }

    @Override
    PactDslJsonBody eachLike(String name) { null }

    @Override
    PactDslJsonBody eachLike(String name, DslPart object) {
      null
    }

    @Override
    PactDslJsonBody eachLike() { null }

    @Override
    PactDslJsonArray eachLike(DslPart object) {
      null
    }

    @Override
    PactDslJsonBody eachLike(String name, int numberExamples) { null }

    @Override
    PactDslJsonBody eachLike(int numberExamples) { null }

    @Override
    PactDslJsonBody minArrayLike(String name, int size) { null }

    @Override
    PactDslJsonBody minArrayLike(int size) { null }

    @Override
    PactDslJsonBody minArrayLike(String name, int size, DslPart object) {
      null
    }

    @Override
    PactDslJsonArray minArrayLike(int size, DslPart object) {
      null
    }

    @Override
    PactDslJsonBody minArrayLike(String name, int size, int numberExamples) { null }

    @Override
    PactDslJsonBody minArrayLike(int size, int numberExamples) { null }

    @Override
    PactDslJsonBody maxArrayLike(String name, int size) { null }

    @Override
    PactDslJsonBody maxArrayLike(int size) { null }

    @Override
    PactDslJsonBody maxArrayLike(String name, int size, DslPart object) {
      null
    }

    @Override
    PactDslJsonArray maxArrayLike(int size, DslPart object) {
      null
    }

    @Override
    PactDslJsonBody maxArrayLike(String name, int size, int numberExamples) { null }

    @Override
    PactDslJsonBody maxArrayLike(int size, int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayLike(String name) { null }

    @Override
    PactDslJsonArray eachArrayLike() { null }

    @Override
    PactDslJsonArray eachArrayLike(String name, int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayLike(int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(int numberExamples, int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, int size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(int numberExamples, int size) { null }

    @Override
    PactDslJsonBody object(String name) { null }

    @Override
    PactDslJsonBody object() { null }

    @Override
    DslPart closeObject() { null }

    @Override
    DslPart close() { null }

    @Override
    PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, DslPart object) {
      null
    }

    @Override
    PactDslJsonBody minMaxArrayLike(int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray minMaxArrayLike(int minSize, int maxSize, DslPart object) {
      null
    }

    @Override
    PactDslJsonBody minMaxArrayLike(String name, int minSize, int maxSize, int numberExamples) {
      null
    }

    @Override
    PactDslJsonBody minMaxArrayLike(int minSize, int maxSize, int numberExamples) {
      null
    }

    @Override
    PactDslJsonArray eachArrayWithMinMaxLike(String name, int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray eachArrayWithMinMaxLike(int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray eachArrayWithMinMaxLike(String name, int numberExamples, int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray eachArrayWithMinMaxLike(int numberExamples, int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray unorderedArray(String name) {
      null
    }

    @Override
    PactDslJsonArray unorderedArray() {
      null
    }

    @Override
    PactDslJsonArray unorderedMinArray(String name, int size) {
      null
    }

    @Override
    PactDslJsonArray unorderedMinArray(int size) {
      null
    }

    @Override
    PactDslJsonArray unorderedMaxArray(String name, int size) {
      null
    }

    @Override
    PactDslJsonArray unorderedMaxArray(int size) {
      null
    }

    @Override
    PactDslJsonArray unorderedMinMaxArray(String name, int minSize, int maxSize) {
      null
    }

    @Override
    PactDslJsonArray unorderedMinMaxArray(int minSize, int maxSize) {
      null
    }

    @Override
    DslPart matchUrl(String name, String basePath, Object... pathFragments) {
      null
    }

    @Override
    DslPart matchUrl(String basePath, Object... pathFragments) {
      null
    }

    @Override
    DslPart arrayContaining(String name) {
      null
    }

    @Override
    DslPart matchUrl2(String name, Object... pathFragments) {
      null
    }

    @Override
    DslPart matchUrl2(Object... pathFragments) {
      null
    }

    @Override
    void setBody(JsonValue body) { }
  }

  @Unroll
  def 'matcher methods generate the correct matcher definition - #matcherMethod'() {
    expect:
    subject."$matcherMethod"(param).toMap(PactSpecVersion.V3) == matcherDefinition

    where:

    matcherMethod         | param        | matcherDefinition
    'regexp'              | '[0-9]+'     | [match: 'regex', regex: '[0-9]+']
    'matchTimestamp'      | 'yyyy-mm-dd' | [match: 'timestamp', timestamp: 'yyyy-mm-dd']
    'matchDate'           | 'yyyy-mm-dd' | [match: 'date', date: 'yyyy-mm-dd']
    'matchTime'           | 'yyyy-mm-dd' | [match: 'time', time: 'yyyy-mm-dd']
    'matchMin'            | 1            | [match: 'type', min: 1]
    'matchMax'            | 1            | [match: 'type', max: 1]
    'includesMatcher'     | 1            | [match: 'include', value: '1']
    'matchMinIgnoreOrder' | 1            | [match: 'ignore-order', min: 1]
    'matchMaxIgnoreOrder' | 1            | [match: 'ignore-order', max: 1]
  }
}
