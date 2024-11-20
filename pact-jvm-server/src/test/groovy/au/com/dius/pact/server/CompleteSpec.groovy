package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.DefaultPactWriter
import au.com.dius.pact.core.model.PactWriter
import au.com.dius.pact.core.model.Pact
import spock.lang.Specification

class CompleteSpec extends Specification {
  def 'getPort'() {
    expect:
    Complete.INSTANCE.getPort(input) == result

    where:

    input          | result
    null           | null
    'null'         | null
    [:]            | null
    [a: 'b']       | null
    [port: '1234'] | '1234'
    [port: 1234]   | '1234'
  }

  def 'apply returns an error if the port is not in the request body'() {
    given:
    def request = new Request()
    def state = new ServerState()

    when:
    def result = Complete.apply(request, state)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if the port is not mapped to a server'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"port": "1234"}')
    def state = new ServerState(['45454': Mock(StatefulMockProvider)])

    when:
    def result = Complete.apply(request, state)

    then:
    result.response.status == 400
  }

  def 'apply returns an error if the corresponding server has no Pact'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"port": "1234"}')
    def mockProvider = Mock(StatefulMockProvider) {
      getPact() >> null
      getSession() >> PactSession.empty
    }
    def state = new ServerState(['1234': mockProvider])

    when:
    def result = Complete.apply(request, state)

    then:
    result.response.status == 400
  }

  def 'apply calls stop on the matching mock server'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"port": "1234"}')
    def pact = new RequestResponsePact(new Provider(), new Consumer())
    def mockProvider = Mock(StatefulMockProvider) {
      getPact() >> pact
      getSession() >> PactSession.empty
      getConfig() >> new MockProviderConfig('localhost', 1234)
    }
    def state = new ServerState(['1234': mockProvider])

    when:
    Complete.apply(request, state)

    then:
    1 * mockProvider.stop()
  }

  def 'apply writes out the Pact file and returns a success if all requests matched'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"port": "1234"}')
    def pact = Mock(Pact) {
      getConsumer() >> new Consumer()
      getProvider() >> new Provider()
    }
    def session = PactSession.empty
    def mockProvider = Mock(StatefulMockProvider) {
      getPact() >> pact
      getSession() >> session
      getConfig() >> new MockProviderConfig('localhost', 1234)
    }
    def state = new ServerState(['1234': mockProvider])
    def mockWriter = Mock(PactWriter)
    Complete.INSTANCE.pactWriter = mockWriter

    when:
    def result = Complete.apply(request, state)

    then:
    1 * mockWriter.writePact(_, pact, _)
    result.response.status == 200
    result.newState.state.empty

    cleanup:
    Complete.INSTANCE.pactWriter = DefaultPactWriter.INSTANCE
  }

  def 'apply does not write out the Pact file and returns an error if not all requests matched'() {
    given:
    def request = new Request()
    request.body = OptionalBody.body('{"port": "1234"}')
    def pact = Mock(Pact) {
      getConsumer() >> new Consumer()
      getProvider() >> new Provider()
    }
    def session = PactSession.empty.recordUnexpected(new Request())
    def mockProvider = Mock(StatefulMockProvider) {
      getPact() >> pact
      getSession() >> session
      getConfig() >> new MockProviderConfig('localhost', 1234)
    }
    def state = new ServerState(['1234': mockProvider])
    def mockWriter = Mock(PactWriter)
    Complete.INSTANCE.pactWriter = mockWriter

    when:
    def result = Complete.apply(request, state)

    then:
    0 * mockWriter.writePact(_, pact, _)
    result.response.status == 400
    result.newState.state.empty

    cleanup:
    Complete.INSTANCE.pactWriter = DefaultPactWriter.INSTANCE
  }
}
