package au.com.dius.pact.consumer.dsl

import spock.lang.Specification
import spock.lang.Unroll

class DslPartSpec extends Specification {

  private DslPart subject = new DslPart('', '') {

    @Override
    protected void putObject(DslPart object) { }

    @Override
    protected void putArray(DslPart object) { }

    @Override
    Object getBody() { null }

    @Override
    PactDslJsonArray array(String name) { null }

    @Override
    PactDslJsonArray array() { null }

    @Override
    DslPart closeArray() { null }

    @Override
    PactDslJsonBody arrayLike(String name) { null }

    @Override
    PactDslJsonBody arrayLike() { null }

    @Override
    PactDslJsonBody eachLike(String name) { null }

    @Override
    PactDslJsonBody eachLike() { null }

    @Override
    PactDslJsonBody eachLike(String name, int numberExamples) { null }

    @Override
    PactDslJsonBody eachLike(int numberExamples) { null }

    @Override
    PactDslJsonBody minArrayLike(String name, Integer size) { null }

    @Override
    PactDslJsonBody minArrayLike(Integer size) { null }

    @Override
    PactDslJsonBody minArrayLike(String name, Integer size, int numberExamples) { null }

    @Override
    PactDslJsonBody minArrayLike(Integer size, int numberExamples) { null }

    @Override
    PactDslJsonBody maxArrayLike(String name, Integer size) { null }

    @Override
    PactDslJsonBody maxArrayLike(Integer size) { null }

    @Override
    PactDslJsonBody maxArrayLike(String name, Integer size, int numberExamples) { null }

    @Override
    PactDslJsonBody maxArrayLike(Integer size, int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayLike(String name) { null }

    @Override
    PactDslJsonArray eachArrayLike() { null }

    @Override
    PactDslJsonArray eachArrayLike(String name, int numberExamples) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayLike(int numberExamples) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMinLike(Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size) {
      return null
    }

    @Override
    PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size) {
      return null
    }

    @Override
    PactDslJsonBody object(String name) {
      return null
    }

    @Override
    PactDslJsonBody object() {
      return null
    }

    @Override
    DslPart closeObject() {
      return null
    }

    @Override
    DslPart close() {
      return null
    }
  }

  @Unroll
  def 'matcher methods generate the correct matcher definition - #matchType'() {
    expect:
    subject."$matcherMethod"(param) == matcherDefinition

    where:

    matcherMethod    | param        | matcherDefinition
    'matchType'      | 'type'       | [match: 'type']
    'matchType'      | 'decimal'    | [match: 'decimal']
    'regexp'         | '[0-9]+'     | [match: 'regex', regex: '[0-9]+']
    'matchTimestamp' | 'yyyy-mm-dd' | [match: 'timestamp', timestamp: 'yyyy-mm-dd']
    'matchDate'      | 'yyyy-mm-dd' | [match: 'date', date: 'yyyy-mm-dd']
    'matchTime'      | 'yyyy-mm-dd' | [match: 'time', time: 'yyyy-mm-dd']
    'matchMin'       | 1            | [match: 'type', min: 1]
    'matchMax'       | 1            | [match: 'type', max: 1]

  }

}
