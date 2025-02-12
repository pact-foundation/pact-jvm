package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// Issue: header method in V4 PactBuilder splits up values #1852
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "1852_provider", pactVersion = PactSpecVersion.V4)
public class AcceptHeaderTest {

  @Pact(consumer="old_dsl")
  V4Pact oldDsl(PactBuilder builder) {
    return builder
      .usingLegacyDsl()
      .uponReceiving("get")
      .method("GET")
      .headers("accept", "application/json, application/*+json")
      .path("/old_dsl")
      .willRespondWith()
      .status(200)
      .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(pactMethod = "oldDsl")
  void runOldDslTest(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl() + "/old_dsl")
      .addHeader("accept", "application/json, application/*+json")
      .execute()
      .returnResponse();
    assertThat(httpResponse.getCode(), is(200));
  }

  @Pact(consumer="new_dsl")
  public V4Pact newDsl(PactBuilder builder) {
    return builder.expectsToReceiveHttpInteraction("get", httpBuilder -> {
      return httpBuilder.withRequest(httpRequestBuilder -> {
        return httpRequestBuilder
          .path("/new_dsl")
          .method("GET")
          .headers("accept", "application/json, application/*+json");
      })
      .willRespondWith(httpResponseBuilder -> httpResponseBuilder.status(200));
    })
    .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "newDsl")
  void runNewDslTest(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl() + "/new_dsl")
      .addHeader("accept", "application/json, application/*+json")
      .execute()
      .returnResponse();
    assertThat(httpResponse.getCode(), is(200));
  }
}
