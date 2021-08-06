package au.com.dius.pact.core.pactbroker

import au.com.dius.pact.core.support.json.JsonParser
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.auth.BasicScheme
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.RedirectExec
import org.apache.hc.client5.http.protocol.RedirectStrategy
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicHeader
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import javax.net.ssl.SSLHandshakeException
import java.util.function.Consumer

@SuppressWarnings(['LineLength', 'UnnecessaryGetter', 'ClosureAsLastMethodParameter'])
class HalClientSpec extends Specification {

  private @Shared HalClient client
  private CloseableHttpClient mockClient

  def setup() {
    mockClient = Mock(CloseableHttpClient)
    client = Spy(HalClient, constructorArgs: ['http://localhost:1234/'])
    client.pathInfo = null
  }

  @SuppressWarnings(['LineLength', 'UnnecessaryBooleanExpression'])
  def 'can parse templated URLS correctly'() {
    expect:
    client.parseLinkUrl(url, options) == parsedUrl

    where:
    url                                                   | options              || parsedUrl
    ''                                                    | [:]                  || ''
    'http://localhost:8080/123456'                        | [:]                  || 'http://localhost:8080/123456'
    'http://docker:5000/pacts/provider/{provider}/latest' | [:]                  || 'http://docker:5000/pacts/provider/%7Bprovider%7D/latest'
    'http://docker:5000/pacts/provider/{provider}/latest' | [provider: 'test']   || 'http://docker:5000/pacts/provider/test/latest'
    'http://docker:5000/{b}/provider/{a}/latest'          | [a: 'a', b: 'b']     || 'http://docker:5000/b/provider/a/latest'
    '{a}://docker:5000/pacts/provider/{b}/latest'         | [a: 'test', b: 'b']  || 'test://docker:5000/pacts/provider/b/latest'
    'http://docker:5000/pacts/provider/{a}{b}'            | [a: 'test/', b: 'b'] || 'http://docker:5000/pacts/provider/test%2Fb'
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'matches authentication scheme case insensitive'() {
    given:
    client.options = [authentication: ['BASIC', '1', '2']]

    when:
    client.setupHttpClient()

    then:
    client.httpClient.credentialsProvider instanceof BasicCredentialsProvider
    client.httpContext == null
  }

  @RestoreSystemProperties
  def 'populates the auth cache if preemptive authentication system property is enabled'() {
    given:
    client.options = [authentication: ['basic', '1', '2']]
    System.setProperty('pact.pactbroker.httpclient.usePreemptiveAuthentication', 'true')
    def host = new HttpHost('http', 'localhost', 1234)

    when:
    client.setupHttpClient()

    then:
    client.httpClient.credentialsProvider instanceof BasicCredentialsProvider
    client.httpContext != null
    client.httpContext.authCache.get(host) instanceof BasicScheme
  }

  def 'retry strategy is added to execution chain of client'() {
    when:
    client.setupHttpClient()

    then:
    client.httpClient.execChain.handler instanceof RedirectExec
    client.httpClient.execChain.handler.redirectStrategy instanceof RedirectStrategy
  }

  def 'throws an exception if the response is 404 Not Found'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 404
    }

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute(_, _, _) >> { req, context, handler -> handler.handleResponse(mockResponse) }
    thrown(NotFoundHalResponse)
  }

  def 'throws an exception if the request fails'() {
    given:
    client.httpClient = mockClient

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute(_, _, _) >> { throw new SSLHandshakeException('PKIX path building failed')  }
    thrown(SSLHandshakeException)
  }

  def 'throws an exception if the response is not JSON'() {
    given:
    client.httpClient = mockClient
    def contentType = new BasicHeader('Content-Type', 'text/plain')
    def mockBody = Mock(HttpEntity) {
      getContentType() >> contentType
    }
    def mockRootResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> mockBody
    }

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockRootResponse) }
    thrown(InvalidHalResponse)
  }

  def 'throws an exception if the _links is not found'() {
    given:
    client.httpClient = mockClient
    def body = new StringEntity('{}', ContentType.APPLICATION_JSON)
    def mockRootResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> body
    }

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockRootResponse) }
    thrown(InvalidHalResponse)
  }

  def 'throws an exception if the required link is not found'() {
    given:
    client.httpClient = mockClient
    def body = new StringEntity('{"_links":{}}', ContentType.APPLICATION_JSON)
    def mockRootResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> body
    }

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockRootResponse) }
    thrown(InvalidHalResponse)
  }

  def 'Handles responses with charset attributes'() {
    given:
    client.httpClient = mockClient
    def mockBody = Mock(HttpEntity) {
      getContentType() >> 'application/hal+json;charset=UTF-8'
      getContent() >> new ByteArrayInputStream('{"_links": {"pb:latest-provider-pacts":{"href":"/link"}}}'.bytes)
    }
    def mockRootResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> mockBody
    }
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{}}', ContentType.create('application/hal+json'))
    }

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockRootResponse) }
    1 * mockClient.execute({ it.uri.path == '/link' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    notThrown(InvalidHalResponse)
  }

  def 'does not throw an exception if the required link is empty'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{"pacts": []}}', ContentType.create('application/hal+json'))
    }

    when:
    def called = false
    client.forAll('pacts') { called = true }

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    !called
  }

  def 'uploading a JSON doc'() {
    given:
    client.httpClient = mockClient
    client.pathInfo = JsonParser.INSTANCE.parseString('{"_links":{"link":{"href":"http://localhost:8080/"}}}')
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
    }

    when:
    def result = client.putJson('link', [:], '{}')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    result instanceof Ok
  }

  def 'uploading a JSON doc returns an error'() {
    given:
    client.httpClient = mockClient
    client.pathInfo = JsonParser.INSTANCE.parseString('{"_links":{"link":{"href":"http://localhost:8080/"}}}')
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 400
      getEntity() >> new StringEntity('{"errors":["1","2","3"]}', ContentType.create('application/json'))
    }

    when:
    def result = client.putJson('link', [:], '{}')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    result instanceof Err
  }

  def 'uploading a JSON doc unsuccessful due to 409'() {
    given:
    client.httpClient = mockClient
    client.pathInfo = JsonParser.INSTANCE.parseString('{"_links":{"link":{"href":"http://localhost:8080/"}}}')
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 409
      getEntity() >> new StringEntity('error line')
    }

    when:
    def result = client.putJson('link', [:], '{}')

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    result instanceof Err
  }

  @Unroll
  def 'failure handling - #description'() {
    given:
    client.httpClient = mockClient
    def resp = [
      getCode: { 400 },
      getReasonPhrase: { 'Not OK' },
      getEntity: { [getContentType: { 'application/json' } ] as HttpEntity }
    ] as ClassicHttpResponse

    expect:
    client.handleFailure(resp, body) { arg1, arg2 -> [arg1, arg2] } == [firstArg, secondArg]

    where:

    description                                | body                               | firstArg | secondArg
    'body is null'                             | null                               | 'FAILED' | '400 Not OK'
    'body is a parsed json doc with no errors' | '{}'                               | 'FAILED' | '400 Not OK'
    'body is a parsed json doc with errors'    | '{"errors":["one","two","three"]}' | 'FAILED' | '400 Not OK - one, two, three'

  }

  @Unroll
  @SuppressWarnings('UnnecessaryGetter')
  def 'post URL returns #success if the response is #status'() {
    given:
    def mockClient = Mock(CloseableHttpClient)
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> status
    }
    mockClient.execute(_, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }

    expect:
    client.postJson('path', 'body').class == expectedResult

    where:

    success   | status | expectedResult
    'success' | 200    | Ok
    'failure' | 400    | Err
  }

  def 'post URL returns a failure result if an exception is thrown'() {
    given:
    def mockClient = Mock(CloseableHttpClient)
    client.httpClient = mockClient

    when:
    def result = client.postJson('path', 'body')

    then:
    1 * mockClient.execute(_, _, _) >> { throw new IOException('Boom!') }
    result instanceof Err
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'post URL delegates to a handler if one is supplied'() {
    given:
    def mockClient = Mock(CloseableHttpClient)
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
    }
    mockClient.execute(_, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }

    when:
    def result = client.postJson('path', 'body') { status, resp -> 'handler was called' }

    then:
    result.value == 'handler was called'
  }

  def 'forAll does nothing if there is no matching link'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{}}', ContentType.create('application/hal+json'))
    }
    def closure = Mock(Consumer)

    when:
    client.forAll('missingLink', closure)

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    0 * closure.accept(_)
  }

  def 'forAll calls the closure with the link data'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{"simpleLink": {"link": "linkData"}}}',
        ContentType.create('application/hal+json'))
    }
    def closure = Mock(Consumer)

    when:
    client.forAll('simpleLink', closure)

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    1 * closure.accept([link: 'linkData'])
  }

  def 'forAll calls the closure with each link data when the link is a collection'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{"multipleLink": [{"href":"one"}, {"href":"two"}, {"href":"three"}]}}',
        ContentType.create('application/hal+json'))
    }
    def closure = Mock(Consumer)

    when:
    client.forAll('multipleLink', closure)

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockResponse) }
    1 * closure.accept([href: 'one'])
    1 * closure.accept([href: 'two'])
    1 * closure.accept([href: 'three'])
  }

  def 'supports templated URLs with slashes in the expanded values'() {
    given:
    def providerName = 'test/provider name-1'
    def tag = 'test/tag name-1'
    client.httpClient = mockClient
    def body = new StringEntity('{"_links":{"pb:latest-provider-pacts-with-tag": ' +
      '{"href": "http://localhost/{provider}/tag/{tag}", "templated": true}}}', ContentType.APPLICATION_JSON)
    def mockRootResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> body
    }
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{"linkA": "ValueA"}}', ContentType.create('application/hal+json'))
    }
    def notFoundResponse = Mock(ClassicHttpResponse) {
      getCode() >> 404
    }

    when:
    client.navigate('pb:latest-provider-pacts-with-tag', provider: providerName, tag: tag)

    then:
    1 * mockClient.execute({ it.uri.path == '/' }, _, _) >> { r, c, handler -> handler.handleResponse(mockRootResponse) }
    1 * mockClient.execute({ it.uri.rawPath == '/test%2Fprovider%20name-1/tag/test%2Ftag%20name-1' }, _, _) >>
      { r, c, handler -> handler.handleResponse(mockResponse) }
    _ * mockClient.execute(_, _, _) >> { r, c, handler -> handler.handleResponse(notFoundResponse) }
    client.pathInfo['_links']['linkA'].serialise() == '"ValueA"'
  }

  def 'handles invalid URL characters when fetching documents from the broker'() {
    given:
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{"_links":{"multipleLink": ["one", "two", "three"]}}',
        ContentType.create('application/hal+json'))
    }

    when:
    def result = client.fetch('https://test.pact.dius.com.au/pacts/provider/Activity Service/consumer/Foo Web Client 2/version/1.0.2').value

    then:
    1 * mockClient.execute({ it.uri.toString() == 'https://test.pact.dius.com.au/pacts/provider/Activity%20Service/consumer/Foo%20Web%20Client%202/version/1.0.2' }, _, _) >>
      { r, c, handler -> handler.handleResponse(mockResponse) }
    result['_links']['multipleLink'].values*.serialise() == ['"one"', '"two"', '"three"']
  }

  @Unroll
  def 'link url test'() {
    given:
    client.pathInfo = JsonParser.INSTANCE.parseString(json)

    expect:
    client.linkUrl(name) == url

    where:

    json                                        | name   | url
    '{}'                                        | 'test' | null
    '{"_links": null}'                          | 'test' | null
    '{"_links": "null"}'                        | 'test' | null
    '{"_links": {}}'                            | 'test' | null
    '{"_links": { "test": null }}'              | 'test' | null
    '{"_links": { "test": "null" }}'            | 'test' | null
    '{"_links": { "test": {} }}'                | 'test' | null
    '{"_links": { "test": { "blah": "123" } }}' | 'test' | null
    '{"_links": { "test": { "href": "123" } }}' | 'test' | '123'
    '{"_links": { "test": { "href": 123 } }}'   | 'test' | '123'
  }

  def 'initialise request adds all the default headers to the request'() {
    given:
    client.defaultHeaders = [
      A: 'a',
      B: 'b'
    ]

    when:
    def request = client.initialiseRequest(new HttpPost('/'))

    then:
    request.headers.collectEntries { [it.name, it.value] } == [A: 'a', B: 'b']
  }

  @Issue('#1388')
  def "don't decode/encode URLs from links"() {
    given:
    def docAttributes = [
      'pb:provider': [
        title: 'Provider',
        name: 'my/provider-name',
        href: 'http://localhost:9292/pacticipants/my%2Fprovider-name'
      ]
    ]
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{}', ContentType.create('application/hal+json'))
    }

    when:
    client.withDocContext(docAttributes).navigate('pb:provider')

    then:
    1 * mockClient.execute({ it.uri.rawPath == '/pacticipants/my%2Fprovider-name' }, _, _) >>
      { r, c, handler -> handler.handleResponse(mockResponse) }
  }

  @Issue('1399')
  def 'navigating with a base URL containing a path'() {
    given:
    HalClient client = Spy(HalClient, constructorArgs: ['http://localhost:1234/subpath/one/two'])
    client.pathInfo = null
    client.httpClient = mockClient
    def mockResponse = Mock(ClassicHttpResponse) {
      getCode() >> 200
      getEntity() >> new StringEntity('{}', ContentType.APPLICATION_JSON)
    }

    when:
    client.navigate()

    then:
    1 * mockClient.execute(_, _, _) >> { req, c, handler ->
      assert req.uri.toString() == 'http://localhost:1234/subpath/one/two/'
      handler.handleResponse(mockResponse)
    }
  }
}
