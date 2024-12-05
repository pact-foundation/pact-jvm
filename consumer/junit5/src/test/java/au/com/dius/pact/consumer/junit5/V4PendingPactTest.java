package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static au.com.dius.pact.consumer.dsl.Matchers.notEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "v4_pending_provider")
public class V4PendingPactTest {

  @Pact(consumer="v4_test_consumer")
  public V4Pact httpInteraction(PactBuilder builder) {
    return builder
      .expectsToReceiveHttpInteraction("Pending interaction", httpBuilder -> {
        return httpBuilder
          .withRequest(requestBuilder -> requestBuilder
            .path("/")
            .method("GET"))
          .willRespondWith(responseBuilder -> responseBuilder
            .status(200)
            .body("{\"responsetest\": true, \"version\": \"v3\"}")
            .header("test", notEmpty("Example"))
          )
          .comment("Marking this as pending")
          .pending(true);
      })
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "httpInteraction")
  void runHttpTest(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getCode(), is(200));
    assertThat(new String(httpResponse.getEntity().getContent().readAllBytes()),
      is(equalTo("{\"responsetest\": true, \"version\": \"v3\"}")));
    assertThat(httpResponse.containsHeader("test"), is(true));
  }
}
