package au.com.dius.pact.provider.groovysupport

import au.com.dius.pact.core.model.ContentType as PactContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.provider.IHttpClientFactory
import au.com.dius.pact.provider.IProviderInfo
import au.com.dius.pact.provider.ProviderClient
import au.com.dius.pact.provider.ProviderInfo
import au.com.dius.pact.provider.ProviderResponse
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicClassicHttpRequest
import org.apache.hc.core5.http.message.BasicHeader
import spock.lang.Issue
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

  def 'setting up headers adds an TEXT content type if none was provided and there is a body'() {
    given:
    def headers = [
      A: ['a'],
      B: ['b'],
      C: ['c']
    ]
    request = new Request('PUT', '/', [:], headers, OptionalBody.body('this is some text'.bytes))

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }
    1 * httpRequest.addHeader('Content-Type', 'text/plain')

    0 * httpRequest._
  }

  def 'setting up headers adds an content type if none was provided and there is a body with content type'() {
    given:
    def headers = [
            A: ['a'],
            B: ['b'],
            C: ['c']
    ]
    request = new Request('PUT', '/', [:], headers, OptionalBody.body('{}'.bytes, PactContentType.JSON))

    when:
    client.setupHeaders(request, httpRequest)

    then:
    1 * httpRequest.containsHeader('Content-Type') >> false
    headers.each {
      1 * httpRequest.addHeader(it.key, it.value[0])
    }
    1 * httpRequest.addHeader('Content-Type', ContentType.APPLICATION_JSON.getMimeType())

    0 * httpRequest._
  }

  def 'setting up headers does not add an TEXT content type if there is no body'() {
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
    0 * httpRequest.addHeader('Content-Type', 'text/plain')

    0 * httpRequest._
  }

  def 'setting up headers does not add an TEXT content type if there is already one'() {
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
    0 * httpRequest.addHeader('Content-Type', 'text/plain')

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
    httpRequest = Mock ClassicHttpRequest
    request = new Request('PUT', '/')

    when:
    client.setupBody(request, httpRequest)

    then:
    0 * httpRequest._
  }

  def 'setting up body sets a string entity if it is not a url encoded form post and there is a body'() {
    given:
    httpRequest = Mock ClassicHttpRequest
    request = new Request('PUT', '/', [:], [:], OptionalBody.body('{}'.bytes))

    when:
    client.setupBody(request, httpRequest)

    then:
    1 * httpRequest.setEntity { it instanceof StringEntity && it.content.text == '{}' }
    0 * httpRequest._
  }

  def 'setting up body sets a string entity  entity if it is a url encoded form post and there is no query string'() {
    given:
    httpRequest = Mock ClassicHttpRequest
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
    httpRequest = Mock ClassicHttpRequest
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
    1 * httpClient.execute({
      it.method == 'POST' && it.authority.hostName == 'state.change' && it.authority.port == 1244
    })
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
    1 * httpClient.execute({
      it.method == 'POST' && it.authority.hostName == 'state.change' && it.authority.port == 1244
    })
    0 * _
  }

  def 'makeStateChangeRequest adds the state change values to the body if postStateInBody is true'() {
    given:
    state = new ProviderState('state one', [a: 'a', b: 1])
    def stateChangeUrl = 'http://state.change:1244'
    def exepectedBody = Json.INSTANCE.prettyPrint([
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
      it.method == 'POST' && it.authority.hostName == 'state.change' && it.authority.port == 1244 &&
        it.entity.content.text == exepectedBody
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
      it.method == 'POST' && it.authority.hostName == 'state.change' && it.authority.port == 1244 &&
        it.uri.toString() == 'http://state.change:1244/?state=state%20one&a=a&b=1&action=setup'
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
    request.uri.toString() == 'http://my_host:8080/'
  }

  def 'handles a closure for the host'() {
    given:
    client.provider.host = { 'my_host_from_closure' }
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.uri.toString() == 'http://my_host_from_closure:8080/'
  }

  def 'handles non-strings for the host'() {
    given:
    client.provider.host = 12345678
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.uri.toString() == 'http://12345678:8080/'
  }

  def 'handles a number for the port'() {
    given:
    client.provider.port = 1234
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.uri.toString() == 'http://localhost:1234/'
  }

  def 'handles a closure for the port'() {
    given:
    client.provider.port = { 2345 }
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.uri.toString() == 'http://localhost:2345/'
  }

  def 'handles strings for the port'() {
    given:
    client.provider.port = '2222'
    def pactRequest = new Request()

    when:
    def request = client.newRequest(pactRequest)

    then:
    request.uri.toString() == 'http://localhost:2222/'
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
    request.uri.toString() == 'http://localhost:8080/tenants/tester%2Ftoken/jobs/external-id'
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
    request.uri.query == 'A=B&A=C'
  }

  def 'handles repeated headers when handling the response'() {
    given:
    def headers = [
      new BasicHeader('Server', 'Apigee-Edge'),
      new BasicHeader('Set-Cookie', 'JSESSIONID=alphabeta120394049; HttpOnly'),
      new BasicHeader('Set-Cookie', 'AWSELBID=baaadbeef6767676767690220; Path=/alpha')
    ] as Header[]
    ClassicHttpResponse response = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getHeaders() >> headers
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

  def 'handles headers with comma-seperated values'() {
    given:
    Header[] headers = [
      new BasicHeader('Server', 'Apigee-Edge'),
      new BasicHeader('Access-Control-Expose-Headers', 'content-length,content-type'),
      new BasicHeader('Access-Control-Expose-Headers', 'accept')
    ] as Header[]
    ClassicHttpResponse response = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getHeaders() >> headers
    }

    when:
    def result = client.handleResponse(response)

    then:
    result.statusCode == 200
    result.headers == [
      Server: ['Apigee-Edge'],
      'Access-Control-Expose-Headers': ['content-length', 'content-type', 'accept']
    ]
  }

  @Issue('#1159')
  def 'do not split header values for known single value headers'() {
    given:
    Header[] headers = [
      new BasicHeader('Set-Cookie', 'JSESSIONID=alphabeta120394049,baaadbeef6767676767690220; Path=/alpha'),
      new BasicHeader('WWW-Authenticate', 'Basic realm="Access to the staging site", charset="UTF-8"'),
      new BasicHeader('Proxy-Authenticate', 'Basic realm="Access to the internal site, A"'),
      new BasicHeader('Date', 'Wed, 21 Oct 2015 07:28:00 GMT'),
      new BasicHeader('Expires', 'Wed, 21 Oct 2015 07:28:00 GMT'),
      new BasicHeader('Last-Modified', 'Wed, 21 Oct 2015 07:28:00 GMT'),
      new BasicHeader('If-Modified-Since', 'Wed, 21 Oct 2015 07:28:00 GMT'),
      new BasicHeader('If-Unmodified-Since', 'Wed, 21 Oct 2015 07:28:00 GMT')
    ] as Header[]
    ClassicHttpResponse response = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getHeaders() >> headers
    }

    when:
    def result = client.handleResponse(response)

    then:
    result.statusCode == 200
    result.headers == [
      'Set-Cookie': ['JSESSIONID=alphabeta120394049,baaadbeef6767676767690220; Path=/alpha'],
      'WWW-Authenticate': ['Basic realm="Access to the staging site", charset="UTF-8"'],
      'Proxy-Authenticate': ['Basic realm="Access to the internal site, A"'],
      'Date': ['Wed, 21 Oct 2015 07:28:00 GMT'],
      'Expires': ['Wed, 21 Oct 2015 07:28:00 GMT'],
      'Last-Modified': ['Wed, 21 Oct 2015 07:28:00 GMT'],
      'If-Modified-Since': ['Wed, 21 Oct 2015 07:28:00 GMT'],
      'If-Unmodified-Since': ['Wed, 21 Oct 2015 07:28:00 GMT']
    ]
  }

  @Issue('#1013')
  def 'If no content type header is present, defaults to text/plain'() {
    given:
    Header[] headers = [] as Header[]
    ClassicHttpResponse response = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getHeaders() >> headers
      getEntity() >> new StringEntity('HELLO', null as ContentType)
    }

    when:
    def result = client.handleResponse(response)

    then:
    result.contentType.toString() == 'text/plain; charset=ISO-8859-1'
  }

  def 'URL decodes the path'() {
    given:
    String path = '%2Fpath%2FTEST+PATH%2F2014-14-06+23%3A22%3A21'
    def request = new Request('GET', path, [:], [:], OptionalBody.body(''.bytes))
    Header[] headers = [] as Header[]
    ClassicHttpResponse response = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getHeaders() >> headers
    }

    when:
    client.makeRequest(request)

    then:
    1 * httpClientFactory.newClient(_) >> httpClient
    1 * httpClient.execute(_, _) >> { method, callback ->
      assert method.uri.path == '/path/TEST PATH/2014-14-06 23:22:21'
      callback.handleResponse(response)
    }
  }

  def 'query parameters must NOT be placed in the body for URL encoded FORM POSTs'() {
    given:
    def request = new Request('POST', '/', PactReaderKt.queryStringToMap('a=1&b=11&c=Hello World'),
      ['Content-Type': [ContentType.APPLICATION_FORM_URLENCODED.toString()]], OptionalBody.body('A=B'.bytes))

    when:
    client.makeRequest(request)

    then:
    1 * httpClientFactory.newClient(_) >> httpClient
    1 * httpClient.execute(_, _) >> { method, callback ->
      assert method.requestUri == '/?a=1&b=11&c=Hello%20World'
      assert method.entity.content.text == 'A=B'
      new ProviderResponse(200)
    }
  }

  @Unroll
  def 'setupBody() needs to take Content-Type header into account (#charset)'() {
    given:
    def headers = ['Content-Type': [contentType]]
    def body = 'ÄÉÌÕÛ'
    def method = new BasicClassicHttpRequest('PUT', '/')

    when:
    client.setupBody(new Request('PUT', '/', [:], headers, OptionalBody.body(body.bytes)), method)

    then:
    method.entity.contentType == contentType
    method.entity.content.getText(charset) == body

    where:

    charset      | contentType
    'UTF-8'      | 'text/plain; charset=UTF-8'
    'ISO-8859-1' | 'text/plain; charset=ISO-8859-1'
  }


  def 'setupBody() Content-Type defaults to plain text with encoding'() {
    given:
    def contentType = 'text/plain; charset=ISO-8859-1'
    def body = 'ÄÉÌÕÛ'
    def request = new Request('PUT', '/', [:], [:], OptionalBody.body(body.bytes))
    def method = new BasicClassicHttpRequest('PUT', '/')

    when:
    client.setupBody(request, method)

    then:
    method.entity.contentType == contentType
    method.entity.content.getText('ISO-8859-1') == body
  }
}
