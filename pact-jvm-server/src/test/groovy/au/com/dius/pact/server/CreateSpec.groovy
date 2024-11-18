package au.com.dius.pact.server

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
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
      ['/data'],
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
      [],
      pact,
      new ServerState(),
      new Config(4444, 'localhost', false, 20000, 40000, true,
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

  def 'apply returns an error if there is no query parameters'() {
    given:
    def request = new Request()
    def serverState = new ServerState()
    def config = new Config()

    when:
    def result = Create.apply(request, serverState, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if there is no state query parameter'() {
    given:
    def request = new Request('GET', '/path', [qp: ['some value']])
    def serverState = new ServerState()
    def config = new Config()

    when:
    def result = Create.apply(request, serverState, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if the state query parameter is empty'() {
    given:
    def request = new Request('GET', '/path', [qp: []])
    def serverState = new ServerState()
    def config = new Config()

    when:
    def result = Create.apply(request, serverState, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if there is no path query parameter'() {
    given:
    def request = new Request('GET', '/path', [state: ['test']])
    def serverState = new ServerState()
    def config = new Config()

    when:
    def result = Create.apply(request, serverState, config)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if the request body is empty'() {
    given:
    def request = new Request('GET', '/path', [state: ['test'], path: ['/test']], [:], OptionalBody.empty())
    def serverState = new ServerState()
    def config = new Config()

    when:
    def result = Create.apply(request, serverState, config)

    then:
    result.response.status == 400
  }
}
