package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ClassicHttpResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'JsonStringAtRootTest', pactVersion = PactSpecVersion.V4)
class JsonStringAtRootTest {
  final static String JOB_ID = '08f7a210-95db-4827-bcc8-d2025ba506cf'

  @Pact(consumer = 'Consumer')
  V4Pact pact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request for some JSON')
      .path('/endpoint')
      .willRespondWith()
      .status(201)
      .body(PactDslJsonRootValue.uuid(JOB_ID))
      .toPact(V4Pact)
  }

  @Test
  void test(MockServer mockServer) {
    ClassicHttpResponse httpResponse = Request.get("${mockServer.url}/endpoint")
      .execute().returnResponse() as ClassicHttpResponse
    assert httpResponse.code == 201
    assert httpResponse.getHeader('content-type').value == 'application/json; charset=UTF-8'
    assert httpResponse.entity.content.text == '"' + JOB_ID + '"'
  }
}
