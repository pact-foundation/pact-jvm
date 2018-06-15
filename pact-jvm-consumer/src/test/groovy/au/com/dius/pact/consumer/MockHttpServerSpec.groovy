package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.model.RequestResponsePact
import spock.lang.IgnoreIf
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

    headers = ['Content-Type': contentTypeHeader]

  }

  def 'with no content type defaults to UTF-8'() {
    expect:
    MockHttpServerKt.calculateCharset([:]).name() == 'UTF-8'
  }

  def 'ignores case with the header name'() {
    expect:
    MockHttpServerKt.calculateCharset(['content-type': 'text/plain; charset=ISO-8859-1']).name() == 'ISO-8859-1'
  }

  @Timeout(60)
  @IgnoreIf({ os.windows })
  def 'handle more than 200 tests'() {
    given:
    def pact = new RequestResponsePact(new Provider(), new Consumer(), [])
    def config = MockProviderConfig.createDefault()

    when:
    201.times { count ->
      def server = mockServer(pact, config)
      server.runAndWritePact(pact, config.pactVersion) { }
    }

    then:
    true
  }

}
