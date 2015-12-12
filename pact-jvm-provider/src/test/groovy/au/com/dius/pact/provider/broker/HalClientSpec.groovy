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

}
