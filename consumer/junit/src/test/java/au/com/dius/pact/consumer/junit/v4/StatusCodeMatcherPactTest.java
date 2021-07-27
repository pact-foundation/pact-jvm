package au.com.dius.pact.consumer.junit.v4;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StatusCodeMatcherPactTest {

  @Rule
  public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", PactSpecVersion.V4, this);

  @Pact(provider="test_provider", consumer="v4_test_consumer")
  public V4Pact createFragment(PactDslWithProvider builder) {
    return builder
      .uponReceiving("test interaction")
        .path("/")
        .method("GET")
      .willRespondWith()
        .successStatus()
        .body("{\"responsetest\": true, \"version\": \"v3\"}")
      .toPact(V4Pact.class);
  }

  @Test
  @PactVerification
  public void runTest() throws IOException {
    Map expectedResponse = Map.of("responsetest", true, "version", "v3");
    assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
  }
}
