package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

// Issue #1727
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "HeadersWithParametersProvider", pactVersion = PactSpecVersion.V3)
public class HeadersWithParametersTest {
  Map<String, String> headers = Map.of(
    "strict-transport-security", "max-age=3600; includeSubDomains; reload",
    "Content-Security-Policy", "default-src: 'none'; frame-ancestors 'none'; base-uri 'self'"
  );
  @Pact(consumer = "HeadersWithParametersConsumer")
  public RequestResponsePact pact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("retrieving header data")
        .path("/path")
        .method("POST")
        .headers(headers)
      .willRespondWith()
        .headers(headers)
        .status(200)
      .toPact();
  }

  @Test
  void testHeaders(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.post(mockServer.getUrl() + "/path")
            .addHeader("strict-transport-security", "max-age=3600; includeSubDomains; reload")
            .addHeader("Content-Security-Policy", "default-src: 'none'; frame-ancestors 'none'; base-uri 'self'")
            .execute().returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(200)));
  }
}
