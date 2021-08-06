package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "v4_test_provider")
public class V4PactBuilderTest {

  @Pact(provider="v4_test_provider", consumer="v4_test_consumer")
  public V4Pact pact(PactBuilder builder) {
    return builder
      .usingLegacyDsl()
      .given("good state")
      .comment("This is a comment")
      .uponReceiving("V4 PactProviderTest test interaction")
      .path("/")
      .method("GET")
      .comment("Another comment")
      .willRespondWith()
      .status(200)
      .body("{\"responsetest\": true, \"version\": \"v3\"}")
      .comment("This is also a comment")
      .toPact(V4Pact.class);
  }

  @Test
  void runTest(MockServer mockServer) throws IOException {
    ClassicHttpResponse httpResponse = (ClassicHttpResponse) Request.get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getCode(), is(200));
    assertThat(new String(httpResponse.getEntity().getContent().readAllBytes()),
      is(equalTo("{\"responsetest\": true, \"version\": \"v3\"}")));
  }
}
