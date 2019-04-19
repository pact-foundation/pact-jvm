package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.Pact
import au.com.dius.pact.consumer.PactProviderRuleMk2
import au.com.dius.pact.consumer.PactVerification
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.model.RequestResponsePact
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import org.junit.Rule
import org.junit.Test

@SuppressWarnings('PublicInstanceField')
class MultiCookieTest {

  @Rule
  public final PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2('multicookie_provider', this)

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
    def http = new HTTPBuilder(mockProvider.url)

    http.post(path: '/provider', requestContentType: ContentType.JSON) { response ->
      assert response.status == 200
      assert response.headers.iterator().findAll { it.name == 'set-cookie' }*.value == [
        'someCookie=someValue; Path=/', 'someOtherCookie=someValue; Path=/', 'someThirdCookie=someValue; Path=/'
      ]
    }
  }
}
