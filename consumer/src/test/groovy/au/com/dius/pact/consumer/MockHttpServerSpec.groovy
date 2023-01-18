package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponsePact
import com.sun.net.httpserver.HttpExchange
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import static au.com.dius.pact.consumer.MockHttpServerKt.mockServer

class MockHttpServerSpec extends Specification {

  @Unroll
  def 'calculated charset test - "#contentTypeHeader"'() {

    expect:
    MockHttpServerKt.calculateCharset(headers).name() == expectedCharset

    where:

    contentTypeHeader               | expectedCharset
    null                            | 'UTF-8'
    'null'                          | 'UTF-8'
    ''                              | 'UTF-8'
    'text/plain'                    | 'UTF-8'
    'text/plain; charset'           | 'UTF-8'
    'text/plain; charset='          | 'UTF-8'
    'text/plain;charset=ISO-8859-1' | 'ISO-8859-1'

    headers = ['Content-Type': [contentTypeHeader]]

  }

  def 'with no content type defaults to UTF-8'() {
    expect:
    MockHttpServerKt.calculateCharset([:]).name() == 'UTF-8'
  }

  def 'ignores case with the header name'() {
    expect:
    MockHttpServerKt.calculateCharset(['content-type': ['text/plain; charset=ISO-8859-1']]).name() == 'ISO-8859-1'
  }

  @Timeout(60)
  @IgnoreIf({ System.env.CI != 'true' })
  def 'handle more than 200 tests'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [])
    def config = MockProviderConfig.createDefault()

    when:
    201.times { count ->
      def server = mockServer(pact, config)
      server.runAndWritePact(pact, config.pactVersion) { s, context -> }
    }

    then:
    true
  }

  @Issue('#1326')
  def 'use the raw path when creating the Pact request'() {
    given:
    def mockServer = new MockHttpServer(new RequestResponsePact(new Provider(), new Consumer(), []),
      MockProviderConfig.createDefault())
    def exchange = Mock(HttpExchange) {
      getRequestHeaders() >> new com.sun.net.httpserver.Headers()
      getRequestURI() >> new URI('http://localhost/endpoint/Some%2FValue')
      getRequestBody() >> new ByteArrayInputStream([] as byte[])
      getRequestMethod() >> 'GET'
    }

    when:
    def request = mockServer.toPactRequest(exchange)

    then:
    request.path == '/endpoint/Some%2FValue'
  }

  def 'IP6 test'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [])
    def config = new MockProviderConfig(hostname, port)

    when:
    def mockServer = mockServerClass.newInstance(pact, config)
    mockServer.start()

    then:
    mockServer.url ==~ url

    cleanup:
    mockServer.stop()

    where:

    mockServerClass | hostname        | port | url
    MockHttpServer  | '[::1]'         | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpServer  | '[::1]'         | 1234 | 'http://ip6-localhost:1234'
    MockHttpServer  | '::1'           | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpServer  | '::1'           | 1235 | 'http://ip6-localhost:1235'
    MockHttpServer  | 'ip6-localhost' | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpServer  | 'ip6-localhost' | 1236 | 'http://ip6-localhost:1236'
    MockHttpsServer | '[::1]'         | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpsServer | '[::1]'         | 1237 | 'http://ip6-localhost:1237'
    MockHttpsServer | '::1'           | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpsServer | '::1'           | 1238 | 'http://ip6-localhost:1238'
    MockHttpsServer | 'ip6-localhost' | 0    | /http:\/\/ip6-localhost:\d+/
    MockHttpsServer | 'ip6-localhost' | 1239 | 'http://ip6-localhost:1239'
    KTorMockServer  | '[::1]'         | 0    | /http:\/\/ip6-localhost:\d+/
    KTorMockServer  | '[::1]'         | 2234 | 'http://ip6-localhost:2234'
    KTorMockServer  | '::1'           | 0    | /http:\/\/ip6-localhost:\d+/
    KTorMockServer  | '::1'           | 2235 | 'http://ip6-localhost:2235'
    KTorMockServer  | 'ip6-localhost' | 0    | /http:\/\/ip6-localhost:\d+/
    KTorMockServer  | 'ip6-localhost' | 2236 | 'http://ip6-localhost:2236'
  }
}
