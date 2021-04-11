package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "test_provider", pactVersion = PactSpecVersion.V4)
public class V4HttpPactTest {

  @Pact(provider="test_provider", consumer="v4_test_consumer")
  public V4Pact createFragment(PactDslWithProvider builder) {
    return builder
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
    HttpResponse httpResponse = Request.Get(mockServer.getUrl()).execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
    assertThat(new String(httpResponse.getEntity().getContent().readAllBytes()),
      is(equalTo("{\"responsetest\": true, \"version\": \"v3\"}")));
  }
}
