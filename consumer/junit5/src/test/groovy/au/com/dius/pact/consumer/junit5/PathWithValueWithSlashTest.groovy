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
@PactTestFor(providerName = 'PathWithValueWithSlashTest', port = '1234', pactVersion = PactSpecVersion.V3)
class PathWithValueWithSlashTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact filesPact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request with a slash in the path')
        .path('/endpoint/Some%2FValue')
      .willRespondWith()
        .status(200)
      .toPact()
  }

  @Test
  void testFiles(MockServer mockServer) {
    HttpResponse httpResponse = Request.get("${mockServer.url}/endpoint/Some%2FValue")
      .execute().returnResponse()
    assert httpResponse.code == 200
  }
}
