package au.com.dius.pact.consumer.junit

import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.junit.Rule
import org.junit.Test

@SuppressWarnings('PublicInstanceField')
class MultiCookieTest {

  @Rule
  public final PactProviderRule mockProvider = new PactProviderRule('multicookie_provider', this)

  @Pact(consumer= 'browser_consumer')
  RequestResponsePact createPact(PactDslWithProvider builder) {
    builder
      .uponReceiving('request to the provider')
        .path('/provider')
        .method('POST')
      .willRespondWith()
        .status(200)
        .matchSetCookie('someCookie', '.*', 'someValue; Path=/')
        .matchSetCookie('someOtherCookie', '.*', 'someValue; Path=/')
        .matchSetCookie('someThirdCookie', '.*', 'someValue; Path=/')
      .toPact()
  }

  @Test
  @PactVerification
  void runTest() {
    def http = HttpBuilder.configure { request.uri = mockProvider.url }

    http.post {
      request.uri.path = '/provider'
      request.contentType = 'application/json'

      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 200
        assert fs.headers.findAll { it.key == 'set-cookie' }*.value == [
          'someCookie=someValue; Path=/', 'someOtherCookie=someValue; Path=/', 'someThirdCookie=someValue; Path=/'
        ]
      }
    }
  }
}
