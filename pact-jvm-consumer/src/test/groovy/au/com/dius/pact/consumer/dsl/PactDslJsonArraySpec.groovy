package au.com.dius.pact.consumer.dsl

import spock.lang.Specification

class PactDslJsonArraySpec extends Specification {

  def 'close must close off all parents and return the root'() {
    given:
      def root = new PactDslJsonArray()
      def obj = new PactDslJsonBody('b', root)
      def array = new PactDslJsonArray('c', obj)

    when:
      def result = array.close()

    then:
      root.closed
      obj.closed
      array.closed
      result.is root
  }

}
