package au.com.dius.pact.core.support

import org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec
import org.apache.hc.client5.http.impl.classic.MainClientExec
import org.apache.hc.client5.http.protocol.RequestDefaultHeaders
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.Method
import spock.lang.Specification

class HttpClientSpec extends Specification {

  def 'when creating a new http client, add any authentication as default headers'() {
    given:
    URI uri = new URI('http://localhost')
    def authentication = ['bearer', '1234abcd']

    when:
    def result = HttpClient.INSTANCE.newHttpClient(authentication, uri, 1, 1, false)
    def defaultHeaders = null
    def execChain = result.component1().execChain
    while (defaultHeaders == null && execChain != null) {
      if (execChain.handler instanceof MainClientExec) {
        def interceptor = execChain.handler.httpProcessor.requestInterceptors.find {
          it instanceof RequestDefaultHeaders
        }
        defaultHeaders = interceptor.defaultHeaders
      } else {
        execChain = execChain.next
      }
    }

    then:
    defaultHeaders[0].name == 'Authorization'
    defaultHeaders[0].value == 'Bearer 1234abcd'
  }

  def 'http client should retry any requests for any method'(Method method) {
    def uri = new URI('http://localhost')
    def request = Mock(HttpRequest)
    request.method >> method
    def client = HttpClient.INSTANCE.newHttpClient(null, uri, 1, 1, false).component1()
    def retryStrategy = null
    def execChain = client.execChain
    while (retryStrategy == null && execChain != null) {
      if (execChain.handler instanceof HttpRequestRetryExec) {
        retryStrategy = execChain.handler.retryStrategy
      } else {
        execChain = execChain.next
      }
    }

    expect:
    retryStrategy.handleAsIdempotent(request) == true

    where:
    method << Method.values()
  }
}
