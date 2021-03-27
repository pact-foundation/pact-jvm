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
}
