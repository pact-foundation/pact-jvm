package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.StringEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderWithDateTime')
class DateTimeWithTimezoneTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pactWithTimezone(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request with some datetime info')
        .method('POST')
        .path('/values')
        .body(new PactDslJsonBody().datetime('datetime', "YYYY-MM-dd'T'HH:mm:ss.SSSxxx"))
      .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().datetime('datetime', "YYYY-MM-dd'T'HH:mm:ss.SSSxxx"))
      .toPact()
  }

  @Test
  void testFiles(MockServer mockServer) {
    HttpResponse httpResponse = Request.Post("${mockServer.url}/values")
      .body(new StringEntity('{"datetime": "' +
        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSSxxx").format(ZonedDateTime.now())
        + '"}', 'application/json', 'UTF-8'))
      .execute().returnResponse()
    assert httpResponse.statusLine.statusCode == 200
  }
}
