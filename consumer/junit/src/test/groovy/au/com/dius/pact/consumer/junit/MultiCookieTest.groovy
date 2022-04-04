package au.com.dius.pact.consumer.junit

import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.support.SimpleHttp
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
    def http = new SimpleHttp(mockProvider.url)

    def response = http.post('/provider', '', '')
    assert response.statusCode == 200
    assert response.headers['set-cookie'] == [
      'someCookie=someValue; Path=/', 'someOtherCookie=someValue; Path=/', 'someThirdCookie=someValue; Path=/'
    ]
  }
}
