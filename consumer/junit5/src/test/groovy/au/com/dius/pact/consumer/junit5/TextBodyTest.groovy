package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslRootValue
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'TextBodyTest', pactVersion = PactSpecVersion.V3)
class TextBodyTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request to fetch current time')
      .path('/v2/current-time')
      .method('GET')
      .willRespondWith()
      .status(200)
      .headers(['content-type': 'text/plain'])
      .body(PactDslRootValue.stringMatcher('\\d+', '100'))
      .toPact()
  }

  @Test
  void test(MockServer mockServer) {
    def httpResponse = Request.get("${mockServer.url}/v2/current-time")
      .execute().returnResponse()
    assert httpResponse.code == 200
    assert httpResponse.entity.content.text == '100'
  }
}
