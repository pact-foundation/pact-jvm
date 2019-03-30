package au.com.dius.pact.server

import scala.collection.JavaConversions
import spock.lang.Specification

import java.nio.file.Paths

@SuppressWarnings(['FactoryMethodName'])
class CreateSpec extends Specification {

  def 'create starts a new server with the provided pact'() {
    given:
    def pact = CreateSpec.getResourceAsStream('/create-pact.json').text

    when:
    def result = Create.create('test state',
      JavaConversions.asScalaBuffer(['/data']).toList(),
      pact, new scala.collection.immutable.HashMap(),
      new Config(4444, 'localhost', false, 20000, 40000, true,
              2, '', '', 8444))

    then:
    result.response().status == 201
    result.response().body.value != '{"port": 8444}'

    cleanup:
    if (result != null) {
      def state = result.newState()
      def values = state.values()
      JavaConversions.asJavaCollection(values).each {
        it.stop()
      }
    }
  }

  def 'create starts a new server with the provided pact, using a keystore'() {
    given:
    def pact = CreateSpec.getResourceAsStream('/create-pact.json').text
    def keystorePath = Paths.get('src/test/resources/keystore/pact-jvm-512.jks').toFile().absolutePath
    def password = 'brentwashere'

    when:
    def result = Create.create('test state',
      JavaConversions.asScalaBuffer([]).toList(),
      pact, new scala.collection.immutable.HashMap(),
      new Config(4444, 'localhost', false, 20000, 40000, true,
              2, keystorePath, password, 8444))

    then:
    result.response().status == 201
    result.response().body.valueAsString() == '{"port": 8444}'

    cleanup:
    if (result != null) {
      JavaConversions.asJavaCollection(result.newState().values()).each {
        it.stop()
      }
    }
  }

}
