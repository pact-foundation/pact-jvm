package au.com.dius.pact.provider.broker

import groovyx.net.http.AuthConfig
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class HalClientSpec extends Specification {

  private HalClient client

  def setup() {
    client = GroovySpy(HalClient, global: true)
  }

  @SuppressWarnings(['LineLength', 'UnnecessaryBooleanExpression'])
  def 'can parse templated URLS correctly'() {
    expect:
    client.parseLinkUrl(url, options) == parsedUrl

    where:
    url                                                   | options              || parsedUrl
    ''                                                    | [:]                  || ''
    'http://localhost:8080/123456'                        | [:]                  || 'http://localhost:8080/123456'
    'http://docker:5000/pacts/provider/{provider}/latest' | [:]                  || 'http://docker:5000/pacts/provider/{provider}/latest'
    'http://docker:5000/pacts/provider/{provider}/latest' | [provider: 'test']   || 'http://docker:5000/pacts/provider/test/latest'
    'http://docker:5000/{b}/provider/{a}/latest'          | [a: 'a', b: 'b']     || 'http://docker:5000/b/provider/a/latest'
    '{a}://docker:5000/pacts/provider/{b}/latest'         | [a: 'test', b: 'b']  || 'test://docker:5000/pacts/provider/b/latest'
    'http://docker:5000/pacts/provider/{a}{b}'            | [a: 'test/', b: 'b'] || 'http://docker:5000/pacts/provider/test/b'
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'matches authentication scheme case insensitive'() {
    given:
    client.options = [authentication: ['BASIC', '1', '2']]
    def mockHttp = Mock(RESTClient)
    client.newHttpClient() >> mockHttp
    def authConfig = Mock(AuthConfig)
    mockHttp.getAuth() >> authConfig

    when:
    client.setupHttpClient()

    then:
    1 * authConfig.basic('1', '2')
  }

  def 'throws an exception if the response is 404 Not Found'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> { throw new NotFoundHalResponse('') }
    }
    client.newHttpClient() >> mockHttp

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    thrown(NotFoundHalResponse)
  }

  def 'throws an exception if the response is not JSON'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
                    headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'text/plain']]
    }
    client.newHttpClient() >> mockHttp

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    thrown(InvalidHalResponse)
  }

  def 'throws an exception if the _links is not found'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'application/hal+json'],
        data: [:]
      ]
    }
    client.newHttpClient() >> mockHttp

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    thrown(InvalidHalResponse)
  }

  def 'throws an exception if the required link is not found'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'application/hal+json'],
        data: [_links: [:]]
      ]
    }
    client.newHttpClient() >> mockHttp

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    thrown(InvalidHalResponse)
  }

  def 'Handles responses with charset attributes'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'application/hal+json;charset=UTF-8'],
        data: [_links: [
          'pb:latest-provider-pacts': [href: '/link']]
        ]
      ]
      get([path: '/link', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'application/hal+json;charset=UTF-8'],
        data: [_links: []]
      ]
    }
    client.newHttpClient() >> mockHttp

    when:
    client.navigate('pb:latest-provider-pacts')

    then:
    notThrown(InvalidHalResponse)
  }

  def 'does not throw an exception if the required link is empty'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json, application/json']]) >> [
        headers: ['Content-Type': 'application/hal+json'],
        data: [_links: [pacts: []]]
      ]
    }
    client.newHttpClient() >> mockHttp

    when:
    def called = false
    client.pacts { called = true }

    then:
    !called
  }

  def 'uploading a JSON doc returns status line if successful'() {
    given:
    def clientOptions = [
      uri: [:],
      response: [:]
    ]
    def mockHttp = Mock(RESTClient) {
      request(Method.PUT, _) >> { args ->
        args[1].delegate = clientOptions
        args[1].resolveStrategy = Closure.DELEGATE_ONLY
        args[1].call()
      }
    }
    client.newHttpClient() >> mockHttp
    client.consumeEntity() >> null

    when:
    def statusLine = new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 200, 'OK')
    def result = []
    def closure = { r, s -> result << r; result << s }
    client.uploadJson('', '', closure)
    clientOptions.response.success.call([getStatusLine: { statusLine }, getEntity: { } ] as HttpResponse)

    then:
    result == ['OK', 'HTTP/1.1 200 OK']
  }

  def 'uploading a JSON doc returns the error if unsuccessful'() {
    given:
    def clientOptions = [
      uri: [:],
      response: [:]
    ]
    def mockHttp = Mock(RESTClient) {
      request(Method.PUT, _) >> { args ->
        args[1].delegate = clientOptions
        args[1].resolveStrategy = Closure.DELEGATE_ONLY
        args[1].call()
      }
    }
    client.newHttpClient() >> mockHttp

    when:
    def statusLine = new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 400, 'Not OK')
    def result = []
    def closure = { r, s -> result << r; result << s }
    client.uploadJson('', '', closure)
    clientOptions.response.failure.call([getStatusLine: { statusLine } ] as HttpResponse, [errors: ['1', '2', '3']])

    then:
    result == ['FAILED', '400 Not OK - 1, 2, 3']
  }

  def 'uploading a JSON doc returns the error if unsuccessful due to 409'() {
    given:
    def clientOptions = [
      uri: [:],
      response: [:]
    ]
    def mockHttp = Mock(RESTClient) {
      request(Method.PUT, _) >> { args ->
        args[1].delegate = clientOptions
        args[1].resolveStrategy = Closure.DELEGATE_ONLY
        args[1].call()
      }
    }
    client.newHttpClient() >> mockHttp

    when:
    def statusLine = new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 409, 'Not OK')
    def result = []
    def closure = { r, s -> result << r; result << s }
    client.uploadJson('', '', closure)
    clientOptions.response.'409'.call([getStatusLine: { statusLine } ] as HttpResponse, new StringReader('error line'))

    then:
    result == ['FAILED', '409 Not OK - error line']
  }

  @Unroll
  def 'failure handling - #description'() {
    given:
    def statusLine = new BasicStatusLine(new ProtocolVersion('HTTP', 1, 1), 400, 'Not OK')
    def resp = [getStatusLine: { statusLine } ] as HttpResponse

    expect:
    client.handleFailure(resp, body) { arg1, arg2 -> [arg1, arg2] } == [firstArg, secondArg]

    where:

    description                                | body                               | firstArg | secondArg
    'body is a reader'                         | new StringReader('line 1\nline 2') | 'FAILED' | '400 Not OK - line 1'
    'body is null'                             | null                               | 'FAILED' | '400 Not OK - Unknown error'
    'body is a parsed json doc with no errors' | [:]                                | 'FAILED' | '400 Not OK - Unknown error'
    'body is a parsed json doc with errors'    | [errors: ['one', 'two', 'three']]  | 'FAILED' | '400 Not OK - one, two, three'

  }

}
