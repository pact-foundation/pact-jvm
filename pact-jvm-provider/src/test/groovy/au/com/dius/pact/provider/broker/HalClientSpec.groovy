package au.com.dius.pact.provider.broker

import groovyx.net.http.AuthConfig
import groovyx.net.http.RESTClient
import spock.lang.Specification

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

  def 'throws an exception if the response is not JSON'() {
    given:
    def mockHttp = Mock(RESTClient) {
      get([path: '/', requestContentType: 'application/json',
                    headers: [Accept: 'application/hal+json']]) >> [headers: ['Content-Type': 'text/plain']]
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
           headers: [Accept: 'application/hal+json']]) >> [
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
           headers: [Accept: 'application/hal+json']]) >> [
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
           headers: [Accept: 'application/hal+json']]) >> [
        headers: ['Content-Type': 'application/hal+json;charset=UTF-8'],
        data: [_links: [
          'pb:latest-provider-pacts': [href: '/link']]
        ]
      ]
      get([path: '/link', requestContentType: 'application/json',
           headers: [Accept: 'application/hal+json']]) >> [
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

}
