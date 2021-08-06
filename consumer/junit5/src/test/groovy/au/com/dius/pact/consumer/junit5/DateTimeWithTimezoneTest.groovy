package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.io.entity.StringEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderWithDateTime', pactVersion = PactSpecVersion.V3)
class DateTimeWithTimezoneTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pactWithTimezone(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request with some datetime info')
        .method('POST')
        .path('/values')
        .body(new PactDslJsonBody().datetime('datetime', "YYYY-MM-dd'T'HH:mm:ss.SSSXXX"))
      .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().datetime('datetime', "YYYY-MM-dd'T'HH:mm:ss.SSSXXX"))
      .toPact()
  }

  @Test
  void testFiles(MockServer mockServer) {
    HttpResponse httpResponse = Request.post("${mockServer.url}/values")
      .body(new StringEntity('{"datetime": "' +
        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSSXXX").format(ZonedDateTime.now())
        + '"}', ContentType.APPLICATION_JSON))
      .execute().returnResponse()
    assert httpResponse.code == 200
  }
}
