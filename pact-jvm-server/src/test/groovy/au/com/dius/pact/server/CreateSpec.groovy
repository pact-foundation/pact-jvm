package au.com.dius.pact.server

import scala.collection.JavaConverters
import spock.lang.Specification

import java.nio.file.Paths

@SuppressWarnings(['FactoryMethodName'])
class CreateSpec extends Specification {

  def 'create starts a new server with the provided pact'() {
    given:
    def pact = CreateSpec.getResourceAsStream('/create-pact.json').text

    when:
    def result = Create.create(
      'test state',
      JavaConverters.asScalaBuffer(['/data']).toList(),
      pact,
      new ServerState(),
      new Config(4444, 'localhost', false, 20000, 40000, true,
              2, '', '', 8444, '', ''))

    then:
    result.response.status == 201
    result.response.body.value != '{"port": 8444}'

    cleanup:
    if (result != null) {
      def state = result.newState
      def values = state.state.values()
      values.each {
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
      JavaConverters.asScalaBuffer([]).toList(),
      pact,
      new ServerState(),
      new au.com.dius.pact.server.Config(4444, 'localhost', false, 20000, 40000, true,
              2, keystorePath, password, 8444, '', ''))

    then:
    result.response.status == 201
    result.response.body.valueAsString() == '{"port": 8444}'

    cleanup:
    if (result != null) {
      result.newState.state.values().each {
        it.stop()
      }
    }
  }
}
