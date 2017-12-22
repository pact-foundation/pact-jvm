package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.model.matchingrules.RuleLogic
import spock.lang.Specification
import spock.lang.Unroll

class PactDslJsonBodySpec extends Specification {

  def 'close must close off all parents and return the root'() {
    given:
      def root = new PactDslJsonBody()
      def array = new PactDslJsonArray('b', '', root)
      def obj = new PactDslJsonBody('c', '', array)

    when:
      def result = obj.close()

    then:
      root.closed
      obj.closed
      array.closed
      result.is root
  }

  @Unroll
  def 'min array like function should set the example size to the min size'() {
    expect:
    obj.close().body.getJSONArray('test').length() == 2

    where:
    obj << [
      new PactDslJsonBody().minArrayLike('test', 2).id(),
      new PactDslJsonBody().minArrayLike('test', 2, PactDslJsonRootValue.id())
    ]
  }

  def 'min array like function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 3, 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'min array like function with root value should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 3, PactDslJsonRootValue.id(), 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'max array like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().maxArrayLike('test', 3, 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'max array like function with root value should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().minArrayLike('test', 4, PactDslJsonRootValue.id(), 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with max like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonBody().eachArrayWithMaxLike('test', 4, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with min function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonBody().eachArrayWithMinLike('test', 2, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'with nested objects, the rule logic value should be copied'() {
    expect:
    body.matchers.matchingRules['.foo.bar'].ruleLogic == RuleLogic.OR

    where:
    body = new PactDslJsonBody().object('foo')
      .or('bar', 42, PM.numberType(), PM.nullValue())
      .closeObject()
  }

  def 'generate the correct JSON when the attribute name is a number'() {
    expect:
    new PactDslJsonBody()
      .stringType('asdf')
      .array('0').closeArray()
      .eachArrayLike('1').closeArray().closeArray()
      .eachArrayWithMaxLike('2', 10).closeArray().closeArray()
      .eachArrayWithMinLike('3', 10).closeArray().closeArray()
      .close().toString() == '{"0":[],"1":[[]],"2":[[]],"3":[[]],"asdf":"string"}'
  }

  def 'generate the correct JSON when the attribute name has a space'() {
    expect:
    new PactDslJsonBody()
      .array('available Options')
        .object()
        .stringType('Material', 'Gold')
      . closeObject()
      .closeArray().toString() == '{"available Options":[{"Material":"Gold"}]}'
  }

}
