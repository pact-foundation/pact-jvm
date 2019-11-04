package au.com.dius.pact.provider.groovysupport

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.support.Json
@SuppressWarnings('UnusedImport')
import au.com.dius.pact.provider.GroovyScalaUtils$
import au.com.dius.pact.provider.IHttpClientFactory
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicHeader
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['ClosureAsLastMethodParameter', 'MethodCount', 'UnnecessaryGetter'])
class ProviderClientSpec extends Specification {

  private ProviderClient client
  private IProviderInfo provider
  private HttpRequest httpRequest
  private ProviderState state
  private IHttpClientFactory httpClientFactory
  private CloseableHttpClient httpClient
  private Request request

  def setup() {
    provider = new ProviderInfo(
      protocol: 'http',
      host: 'localhost',
      port: 8080,
      path: '/'
    )
    httpClient = Mock CloseableHttpClient
    httpClientFactory = Mock IHttpClientFactory
    client = Spy(ProviderClient, constructorArgs: [provider, httpClientFactory])
    httpRequest = Mock HttpRequest
    state = new ProviderState('provider state')
  }

  def 'setting up headers does nothing if there are no headers'() {
    given:
    request = new Request('PUT', '/')

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    0 * httpRequest._
  }

  def 'setting up headers copies all headers without modification'() {
    given:
    def headers = [
      'Content-Type': [ContentType.APPLICATION_ATOM_XML.toString()],
      A: ['a'],
      B: ['b'],
      C: ['c']
    ]
    request = new Request('PUT', '/', [:], headers)

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> true
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }

    0 * httpRequest._
  }

  def 'setting up headers adds an JSON content type if none was provided and there is a body'() {
    given:
    def headers = [
      A: ['a'],
      B: ['b'],
      C: ['c']
    ]
    request = new Request('PUT', '/', [:], headers, OptionalBody.body('{}'.bytes))

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }
    1 * httpRequest.addHeader('Content-Type', 'application/json')

    0 * httpRequest._
  }

  def 'setting up headers does not add an JSON content type if there is no body'() {
    given:
    def headers = [
      A: ['a'],
      B: ['b'],
      C: ['c']
    ]
    request = new Request('PUT', '/', [:], headers)

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }
    0 * httpRequest.addHeader('Content-Type', 'application/json')

    0 * httpRequest._
  }

  def 'setting up headers does not add an JSON content type if there is already one'() {
    given:
    def headers = [
      A: ['a'],
      B: ['b'],
      'content-type': ['c']
    ]
    request = new Request('PUT', '/', [:], headers, OptionalBody.body('C'.bytes))

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> true
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }
    0 * httpRequest.addHeader('Content-Type', 'application/json')

    0 * httpRequest._
  }

  def 'setting up body does nothing if the request is not an instance of HttpEntityEnclosingRequest'() {
    when:
    client.setupBody(new Request(), httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up body does nothing if it is not a post and there is no body'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    request = new Request('PUT', '/')

    when:
    client.setupBody(request, httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up body sets a string entity if it is not a url encoded form post and there is a body'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    request = new Request('PUT', '/', [:], [:], OptionalBody.body('{}'.bytes))

    when:
    client.setupBody(request, httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == '{}' }
    0 * httpRequest._
  }

  def 'setting up body sets a string entity  entity if it is a url encoded form post and there is no query string'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    request = new Request('POST', '/', [:], ['Content-Type': [ContentType.APPLICATION_FORM_URLENCODED.mimeType]],
      OptionalBody.body('A=B'.bytes))

    when:
    client.setupBody(request, httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == 'A=B' }
    0 * httpRequest._
  }

  def 'setting up body sets a StringEntity entity if it is urlencoded form post and there is a query string'() {
    given:
    httpRequest = Mock HttpEntityEnclosingRequest
    request = new Request('POST', '/', ['A': ['B', 'C']], ['Content-Type': ['application/x-www-form-urlencoded']],
      OptionalBody.body('A=B'.bytes))

    when:
    client.setupBody(request, httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == 'A=B' }
    0 * httpRequest._
  }

  @Unroll
  @SuppressWarnings('UnnecessaryBooleanExpression')
  def 'request is a url encoded form post'() {
    expect:
    def request = new Request(method, '/', ['A': ['B', 'C']], ['Content-Type': [contentType]],
      OptionalBody.body('A=B'.bytes))
    ProviderClient.urlEncodedFormPost(request) == urlEncodedFormPost

    where:
    method      | contentType                         || urlEncodedFormPost
    'POST'      | 'application/x-www-form-urlencoded' || true
    'post'      | 'application/x-www-form-urlencoded' || true
    'PUT'       | 'application/x-www-form-urlencoded' || false
    'GET'       | 'application/x-www-form-urlencoded' || false
    'OPTION'    | 'application/x-www-form-urlencoded' || false
    'HEAD'      | 'application/x-www-form-urlencoded' || false
    'PATCH'     | 'application/x-www-form-urlencoded' || false
    'DELETE'    | 'application/x-www-form-urlencoded' || false
    'TRACE'     | 'application/x-www-form-urlencoded' || false
    'POST'      | 'application/javascript'            || false
  }

  def 'execute request filter does nothing if there is no request filter'() {
    given:
    provider.requestFilter = null

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes any groovy closure'() {
    given:
    Boolean closureCalled = false
    provider.requestFilter = { request ->
      closureCalled = true
      httpRequest.addHeader('A', 'B')
    }

    when:
    client.executeRequestFilter(httpRequest)

    then:
    closureCalled
    1 * httpRequest.addHeader('A', 'B')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes any scala closure'() {
    given:
    provider.requestFilter = GroovyScalaUtils$.MODULE$.testRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Scala', 'Was Called')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter defaults to executing a groovy script'() {
    given:
    provider.requestFilter = 'request.addHeader("Groovy", "Was Called")'

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Groovy', 'Was Called')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes any Java Consumer'() {
    given:
    provider.requestFilter = GroovyJavaUtils.consumerRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Java Consumer', 'was called')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes a Java Function'() {
    given:
    provider.requestFilter = GroovyJavaUtils.functionRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Java Function', 'was called')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter rejects anything with more than one parameter'() {
    given:
    provider.requestFilter = GroovyJavaUtils.function2RequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    thrown(IllegalArgumentException)
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes any Callable Function'() {
    given:
    provider.requestFilter = GroovyJavaUtils.callableRequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter throws an exception invalid Java Function parameters'() {
    given:
    provider.requestFilter = GroovyJavaUtils.invalidFunction2RequestFilter()

    when:
    client.executeRequestFilter(httpRequest)

    then:
    thrown(IllegalArgumentException)
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'execute request filter executes any google collection closure'() {
    given:
    provider.requestFilter = new org.apache.commons.collections4.Closure() {
      @Override
      void execute(Object request) {
        request.addHeader('Apache Collections Closure', 'Was Called')
      }
    }

    when:
    client.executeRequestFilter(httpRequest)

    then:
    1 * httpRequest.addHeader('Apache Collections Closure', 'Was Called')
    1 * client.executeRequestFilter(_)
    0 * _
  }

  def 'makeStateChangeRequest does nothing if there is no state change URL'() {
    given:
    def stateChangeUrl = null

    when:
    client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)

    then:
    1 * client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)
    0 * _
  }

  def 'makeStateChangeRequest posts the state change if there is a state change URL'() {
    given:
    def stateChangeUrl = 'http://state.change:1244'

    when:
    client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)

    then:
    1 * client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)
    1 * httpClientFactory.newClient(provider) >> httpClient
    1 * httpClient.execute({ it.method == 'POST' && it.requestLine.uri == stateChangeUrl })
    0 * _
  }

  def 'makeStateChangeRequest posts the state change if there is a state change URL and it is a URI'() {
    given:
    def stateChangeUrl = new URI('http://state.change:1244')

    when:
    client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)

    then:
    1 * client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)
    1 * httpClientFactory.newClient(provider) >> httpClient
    1 * httpClient.execute({ it.method == 'POST' && it.requestLine.uri == stateChangeUrl.toString() })
    0 * _
  }

  def 'makeStateChangeRequest adds the state change values to the body if postStateInBody is true'() {
    given:
    state = new ProviderState('state one', [a: 'a', b: 1])
    def stateChangeUrl = 'http://state.change:1244'
    def exepectedBody = Json.INSTANCE.gsonPretty.toJson([
      state: 'state one',
      params: [a: 'a', b: 1],
      action: 'setup'
    ])

    when:
    client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)

    then:
    1 * client.makeStateChangeRequest(stateChangeUrl, state, true, true, true)
    1 * httpClientFactory.newClient(provider) >> httpClient
    1 * httpClient.execute({
      it.method == 'POST' && it.requestLine.uri == stateChangeUrl && it.entity.content.text == exepectedBody
    })
    0 * _
  }

  def 'makeStateChangeRequest adds the state change values to the query parameters if postStateInBody is false'() {
    given:
    state = new ProviderState('state one', [a: 'a', b: 1])
    def stateChangeUrl = 'http://state.change:1244'

    when:
    client.makeStateChangeRequest(stateChangeUrl, state, false, true, true)

    then:
    1 * client.makeStateChangeRequest(stateChangeUrl, state, false, true, true)
    1 * httpClientFactory.newClient(provider) >> httpClient
    1 * httpClient.execute({
      it.method == 'POST' && it.requestLine.uri == 'http://state.change:1244?state=state+one&a=a&b=1&action=setup'
    })
    0 * _
  }

  def 'handles a string for the host'() {
    given:
    client.provider.host = 'my_host'
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://my_host:8080/'
  }

  def 'handles a closure for the host'() {
    given:
    client.provider.host = { 'my_host_from_closure' }
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://my_host_from_closure:8080/'
  }

  def 'handles non-strings for the host'() {
    given:
    client.provider.host = 12345678
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://12345678:8080/'
  }

  def 'handles a number for the port'() {
    given:
    client.provider.port = 1234
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://localhost:1234/'
  }

  def 'handles a closure for the port'() {
    given:
    client.provider.port = { 2345 }
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://localhost:2345/'
  }

  def 'handles strings for the port'() {
    given:
    client.provider.port = '2222'
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://localhost:2222/'
  }

  def 'fails in an appropriate way if the port is unable to be converted to an integer'() {
    given:
    client.provider.port = 'this is not a port'
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    thrown(NumberFormatException)
  }

  def 'does not decode the path if pact.verifier.disableUrlPathDecoding is set'() {
    given:
    def pactRequest = new Request()
    pactRequest.path = '/tenants/tester%2Ftoken/jobs/external-id'
    client.systemPropertySet('pact.verifier.disableUrlPathDecoding') >> true

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.toString() == 'http://localhost:8080/tenants/tester%2Ftoken/jobs/external-id'
  }

  @Unroll
  def 'Provider base path should be stripped of any trailing slash - #basePath'() {
    expect:
    ProviderClient.stripTrailingSlash(basePath) == path

    where:

    basePath     | path
    ''           | ''
    'path'       | 'path'
    '/path'      | '/path'
    'path/path'  | 'path/path'
    '/'          | ''
    'path/'      | 'path'
    '/path/'     | '/path'
    'path/path/' | 'path/path'

  }

  def 'includes query parameters when it is a form post'() {
    given:
    def pactRequest = new Request('POST', '/', ['A': ['B', 'C']],
      ['Content-Type': 'application/x-www-form-urlencoded'],
      OptionalBody.body('A=B'.bytes))

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.URI.query == 'A=B&A=C'
  }

  def 'handles repeated headers when handling the response'() {
    given:
    StatusLine statusLine = new BasicStatusLine(new ProtocolVersion('http', 1, 1), 200, 'OK')
    Header[] headers = [
      new BasicHeader('Server', 'Apigee-Edge'),
      new BasicHeader('Set-Cookie', 'JSESSIONID=alphabeta120394049; HttpOnly'),
      new BasicHeader('Set-Cookie', 'AWSELBID=baaadbeef6767676767690220; Path=/alpha')
    ] as Header[]
    HttpResponse response = Mock(HttpResponse) {
      getStatusLine() >> statusLine
      getAllHeaders() >> headers
    }

    when:
    def result = client.handleResponse(response)

    then:
    result.statusCode == 200
    result.headers == [
      Server: ['Apigee-Edge'],
      'Set-Cookie': ['JSESSIONID=alphabeta120394049; HttpOnly', 'AWSELBID=baaadbeef6767676767690220; Path=/alpha']
    ]
  }

}
