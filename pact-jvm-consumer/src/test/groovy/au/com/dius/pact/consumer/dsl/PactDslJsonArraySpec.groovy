package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.model.matchingrules.RuleLogic
import spock.lang.Specification

class PactDslJsonArraySpec extends Specification {

  def 'close must close off all parents and return the root'() {
    given:
      def root = new PactDslJsonArray()
      def obj = new PactDslJsonBody('b', '', root)
      def array = new PactDslJsonArray('c', '', obj)

    when:
      def result = array.close()

    then:
      root.closed
      obj.closed
      array.closed
      result.is root
  }

  def 'min array like function should set the example size to the min size'() {
    expect:
    obj.close().body.get(0).length() == 2

    where:
    obj = new PactDslJsonArray().minArrayLike(2).id()
  }

  def 'min array like function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonArray().minArrayLike(3, 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'max array like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonArray().maxArrayLike(3, 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'static min array like function should validate the number of examples match the min size'() {
    when:
    PactDslJsonArray.arrayMinLike(3, 2)

    then:
    thrown(IllegalArgumentException)
  }

  def 'static max array like function should validate the number of examples match the max size'() {
    when:
    PactDslJsonArray.arrayMaxLike(3, 4)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with max like function should validate the number of examples match the max size'() {
    when:
    new PactDslJsonArray().eachArrayWithMaxLike(4, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'each array with min function should validate the number of examples match the min size'() {
    when:
    new PactDslJsonArray().eachArrayWithMinLike(2, 3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'with nested objects, the rule logic value should be copied'() {
    expect:
    body.matchers.matchingRules['[0][*].foo.bar'].ruleLogic == RuleLogic.OR

    where:
    body = new PactDslJsonArray()
      .eachLike()
        .object('foo')
          .or('bar', 42, PM.numberType(), PM.nullValue())
        .closeObject()
      .closeObject()
      .closeArray()
  }

}
