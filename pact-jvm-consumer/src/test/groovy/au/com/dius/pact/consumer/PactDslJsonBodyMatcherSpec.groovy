package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import groovy.json.JsonSlurper
import spock.lang.Specification

class PactDslJsonBodyMatcherSpec extends Specification {

  private PactDslJsonBody subject

  def setup() {
      subject = new PactDslJsonBody()
  }

  def 'String Matcher Throws Exception If The Example Does Not Match The Pattern'() {
    when:
    subject.stringMatcher('name', '[a-z]+', 'dfhdsjf87fdjh')

    then:
    thrown(InvalidMatcherException)
  }

  def 'Hex Matcher Throws Exception If The Example Is Not A Hexadecimal Value'() {
    when:
    subject.hexValue('name', 'dfhdsjf87fdjh')

    then:
    thrown(InvalidMatcherException)
  }

  def 'Uuid Matcher Throws Exception If The Example Is Not An Uuid'() {
    when:
    subject.uuid('name', 'dfhdsjf87fdjh')

    then:
    thrown(InvalidMatcherException)
  }

  def 'each like allows the number of examples to be set'() {
    given:
    subject = new PactDslJsonBody()
      .eachLike('data', 2)
        .date('defDate')
        .decimalType('cost')
        .closeObject()
      .closeArray()

    when:
    def result = new JsonSlurper().parseText(subject.body.toString())

    then:
    result.data.size == 2
    result.data.every { it.keySet() == ['defDate', 'cost'] as Set }
  }

  def 'min like allows the number of examples to be set'() {
    given:
    subject = new PactDslJsonBody()
      .minArrayLike('data', 1, 2)
        .date('defDate')
        .decimalType('cost')
        .closeObject()
      .closeArray()

    when:
    def result = new JsonSlurper().parseText(subject.body.toString())

    then:
    result.data.size == 2
    result.data.every { it.keySet() == ['defDate', 'cost'] as Set }
  }

  def 'max like allows the number of examples to be set'() {
    given:
    subject = new PactDslJsonBody()
      .maxArrayLike('data', 10, 2)
        .date('defDate')
        .decimalType('cost')
        .closeObject()
      .closeArray()

    when:
    def result = new JsonSlurper().parseText(subject.body.toString())

    then:
    result.data.size == 2
    result.data.every { it.keySet() == ['defDate', 'cost'] as Set }
  }

}
