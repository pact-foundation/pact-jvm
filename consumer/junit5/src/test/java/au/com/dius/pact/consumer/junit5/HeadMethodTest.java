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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "HeadMethodProvider")
public class HeadMethodTest {
  @Pact(consumer = "HeadMethodConsumer")
  public V4Pact pact(PactBuilder builder) {
    return builder
      .expectsToReceiveHttpInteraction("HEAD request",
        interaction -> interaction
          .withRequest(request -> request.path("/v1/my/path").method("HEAD"))
          .willRespondWith(response -> response.status(200)))
      .toPact();
  }

  @Test
  void testPact(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.head(mockServer.getUrl() + "/v1/my/path")
      .execute()
      .returnResponse();
    assertThat(httpResponse.getCode(), is(equalTo(200)));
  }
}
