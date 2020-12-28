package au.com.dius.pact.core.support

import spock.lang.Specification

class HttpClientSpec extends Specification {

  def 'when creating a new http client, add any authentication as default headers'() {
    given:
    URI uri = new URI('http://localhost')
    def authentication = ['bearer', '1234abcd']

    when:
    def result = HttpClient.INSTANCE.newHttpClient(authentication, uri, 1, 1)
    def defaultHeaders = result.component1().closeables[0].this$0.defaultHeaders

    then:
    defaultHeaders[0].name == 'Authorization'
    defaultHeaders[0].value == 'Bearer 1234abcd'
  }
}
