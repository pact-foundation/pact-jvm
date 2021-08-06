package au.com.dius.pact.core.support

import org.apache.hc.client5.http.impl.classic.ProtocolExec
import org.apache.hc.client5.http.protocol.RequestDefaultHeaders
import spock.lang.Specification

class HttpClientSpec extends Specification {

  def 'when creating a new http client, add any authentication as default headers'() {
    given:
    URI uri = new URI('http://localhost')
    def authentication = ['bearer', '1234abcd']

    when:
    def result = HttpClient.INSTANCE.newHttpClient(authentication, uri, 1, 1)
    def defaultHeaders = null
    def execChain = result.component1().execChain
    while (defaultHeaders == null && execChain != null) {
      if (execChain.handler instanceof ProtocolExec) {
        def interceptor = execChain.handler.httpProcessor.requestInterceptors.find { it instanceof RequestDefaultHeaders }
        defaultHeaders = interceptor.defaultHeaders
      } else {
        execChain = execChain.next
      }
    }

    then:
    defaultHeaders[0].name == 'Authorization'
    defaultHeaders[0].value == 'Bearer 1234abcd'
  }
}
