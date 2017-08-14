package au.com.dius.pact.server

import scala.collection.JavaConversions
import spock.lang.Specification

@SuppressWarnings(['FactoryMethodName'])
class CreateSpec extends Specification {

  def 'create starts a new server with the provided pact'() {
    given:
    def pact = CreateSpec.getResourceAsStream('/create-pact.json').text

    when:
    def result = Create.create('test state', pact, new scala.collection.immutable.HashMap(),
      new Config(4444, 'localhost', false, 20000, 40000, true, 2))

    then:
    result.response().status == 201

    cleanup:
    JavaConversions.asJavaCollection(result.newState().values()).each {
      it.stop()
    }
  }

}
