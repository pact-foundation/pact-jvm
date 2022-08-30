package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.StringEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'NumberMatcherBodyTest', pactVersion = PactSpecVersion.V3)
class NumberMatcherBodyTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request to fetch a number')
        .path('/path')
        .method('POST')
        .body(new PactDslJsonBody().integerMatching("num", "\\d{5}", 12345))
      .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().decimalMatching("num", '\\d+\\.\\d{2}', 100.02))
      .toPact()
  }

  @Test
  void test(MockServer mockServer) {
    def httpResponse = Request.post("${mockServer.url}/path")
      .body(new StringEntity("{\"num\": 12345}", ContentType.APPLICATION_JSON))
      .execute().returnResponse()
    assert httpResponse.code == 200
    assert httpResponse.entity.content.text == '{"num":100.02}'
  }
}
