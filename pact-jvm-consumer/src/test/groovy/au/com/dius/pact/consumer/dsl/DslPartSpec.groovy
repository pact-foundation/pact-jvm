package au.com.dius.pact.consumer.dsl

import spock.lang.Specification
import spock.lang.Unroll

class DslPartSpec extends Specification {

  @SuppressWarnings('MethodCount')
  private final DslPart subject = new DslPart('', '') {

    @Override
    protected void putObject(DslPart object) { }

    @Override
    protected void putArray(DslPart object) { }

    Object body = null

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
    PactDslJsonArray eachArrayLike(String name, int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayLike(int numberExamples) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(String name, int numberExamples, Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMaxLike(int numberExamples, Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(String name, int numberExamples, Integer size) { null }

    @Override
    PactDslJsonArray eachArrayWithMinLike(int numberExamples, Integer size) { null }

    @Override
    PactDslJsonBody object(String name) { null }

    @Override
    PactDslJsonBody object() { null }

    @Override
    DslPart closeObject() { null }

    @Override
    DslPart close() { null }
  }

  @Unroll
  def 'matcher methods generate the correct matcher definition - #matcherMethod'() {
    expect:
    subject."$matcherMethod"(param).toMap() == matcherDefinition

    where:

    matcherMethod     | param        | matcherDefinition
    'regexp'          | '[0-9]+'     | [match: 'regex', regex: '[0-9]+']
    'matchTimestamp'  | 'yyyy-mm-dd' | [match: 'timestamp', timestamp: 'yyyy-mm-dd']
    'matchDate'       | 'yyyy-mm-dd' | [match: 'date', date: 'yyyy-mm-dd']
    'matchTime'       | 'yyyy-mm-dd' | [match: 'time', time: 'yyyy-mm-dd']
    'matchMin'        | 1            | [match: 'type', min: 1]
    'matchMax'        | 1            | [match: 'type', max: 1]
    'includesMatcher' | 1            | [match: 'include', value: '1']

  }

}
