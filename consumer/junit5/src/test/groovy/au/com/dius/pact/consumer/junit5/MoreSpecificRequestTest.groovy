package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.HttpResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'TestProvider', pactVersion = PactSpecVersion.V3)
// This is a test for issue https://github.com/pact-foundation/pact-reference/issues/69
class MoreSpecificRequestTest {
  @Pact(consumer = 'TestConsumer')
  RequestResponsePact authenticationTest(PactDslWithProvider builder) {
    builder
      .given('is not authenticated')
      .uponReceiving('a request for all animals')
      .path('/animals/available')
      .willRespondWith()
      .status(401)
      .given('is authenticated')
      .uponReceiving('a request for all animals')
      .path('/animals/available')
      .headers([Authorization: 'Bearer token'])
      .willRespondWith()
      .status(200)
      .toPact()
  }

  @Test
  @PactTestFor(pactMethod = 'authenticationTest')
  void testFiles(MockServer mockServer) {
    HttpResponse httpResponse = Request.get("${mockServer.url}/animals/available")
      .execute().returnResponse()
    assert httpResponse.code == 401
    httpResponse = Request.get("${mockServer.url}/animals/available")
      .addHeader('Authorization', 'Bearer token')
      .execute().returnResponse()
    assert httpResponse.code == 200
  }
}
