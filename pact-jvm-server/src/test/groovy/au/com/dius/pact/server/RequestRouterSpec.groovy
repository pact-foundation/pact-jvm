package au.com.dius.pact.server

import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import spock.lang.Specification

class RequestRouterSpec extends Specification {
  def 'matchPath with empty state'() {
    given:
    def request = new Request('GET', '/1234')
    def state = new ServerState()

    expect:
    RequestRouter.INSTANCE.matchPath(request, state) == null
  }

  def 'matchPath with equal state'() {
    given:
    def request = new Request('GET', '/1234')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    expect:
    RequestRouter.INSTANCE.matchPath(request, state) == provider
  }

  def 'matchPath with not matching state'() {
    given:
    def request = new Request('GET', '/abcd')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    expect:
    RequestRouter.INSTANCE.matchPath(request, state) == null
  }

  def 'matchPath with matching state'() {
    given:
    def request = new Request('GET', '/12345678')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    expect:
    RequestRouter.INSTANCE.matchPath(request, state) == provider
  }

  def 'handlePactRequest calls handle request on the matching provider'() {
    given:
    def request = new Request('GET', '/1234')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    when:
    def response = RequestRouter.INSTANCE.handlePactRequest(request, state)

    then:
    1 * provider.handleRequest(request) >> new Response(222)
    0 * provider2.handleRequest(_)
    response.status == 222
  }

  def 'handlePactRequest with no matching provider'() {
    given:
    def request = new Request('GET', '/abcd')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    when:
    def response = RequestRouter.INSTANCE.handlePactRequest(request, state)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response == null
  }

  def 'pactDispatch returns 404 if no matching provider'() {
    given:
    def request = new Request('GET', '/abcd')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])

    when:
    def response = RequestRouter.INSTANCE.pactDispatch(request, state)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.status == 404
  }

  def 'dispatch sends / requests to the ListServers controller'() {
    given:
    def request = new Request('GET', '/')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/2345': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 200
    response.response.body.valueAsString() == '{"ports": [1234, 2345], "paths": ["/1234", "/2345"]}'
  }

  def 'dispatch sends /create requests to the Create controller'() {
    given:
    def request = new Request('GET', '/create')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/create/other': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 400
    response.response.body.valueAsString() == '{"error": "please provide state param and path param and pact body"}'
  }

  def 'dispatch sends /create/* requests to the Create controller'() {
    given:
    def request = new Request('GET', '/create/other')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/create/o': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 400
    response.response.body.valueAsString() == '{"error": "please provide state param and path param and pact body"}'
  }

  def 'dispatch sends /complete requests to the Complete controller'() {
    given:
    def request = new Request('GET', '/complete')
    request.body = OptionalBody.body('{"port":"1234"}')
    def provider = Mock(StatefulMockProvider) {
      getSession() >> PactSession.empty
      getPact() >> new RequestResponsePact(new Provider(), new Consumer())
      getConfig() >> new MockProviderConfig()
    }
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/complete/other': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    1 * provider.stop()
    0 * provider2.handleRequest(_)
    response.response.status == 200
  }

  def 'dispatch sends /complete/* requests to the Complete controller'() {
    given:
    def request = new Request('GET', '/complete/other')
    request.body = OptionalBody.body('{"port":"1234"}')
    def provider = Mock(StatefulMockProvider) {
      getSession() >> PactSession.empty
      getPact() >> new RequestResponsePact(new Provider(), new Consumer())
      getConfig() >> new MockProviderConfig()
    }
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/complete/o': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    1 * provider.stop()
    0 * provider2.handleRequest(_)
    response.response.status == 200
  }

  def 'dispatch sends /publish requests to the Publish controller'() {
    given:
    def request = new Request('GET', '/publish')
    request.body = OptionalBody.body('{}')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/publish/other': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 500
    response.response.body.valueAsString() == '{"error" : "Broker url not correctly configured please run server with -b or --broker \'http://pact-broker.adomain.com\' option" }'
  }

  def 'dispatch sends /publish/* requests to the Publish controller'() {
    given:
    def request = new Request('GET', '/publish/other')
    request.body = OptionalBody.body('{}')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/publish/o': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 500
    response.response.body.valueAsString() == '{"error" : "Broker url not correctly configured please run server with -b or --broker \'http://pact-broker.adomain.com\' option" }'
  }

  def 'dispatch sends all other requests to pactDispatch'() {
    given:
    def request = new Request('GET', '/other')
    def provider = Mock(StatefulMockProvider)
    def provider2 = Mock(StatefulMockProvider)
    def state = new ServerState([
      '1234': provider,
      '2345': provider2,
      '/1234': provider,
      '/publish/o': provider2
    ])
    def config = new Config()

    when:
    def response = RequestRouter.dispatch(request, state, config)

    then:
    0 * provider.handleRequest(_)
    0 * provider2.handleRequest(_)
    response.response.status == 404
  }
}
