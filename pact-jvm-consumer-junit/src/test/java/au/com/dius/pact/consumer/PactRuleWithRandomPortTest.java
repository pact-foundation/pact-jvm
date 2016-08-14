package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactRuleWithRandomPortTest {

    @Rule
    public PactProviderRule rule = new PactProviderRule("test_provider", this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        PactFragment pactFragment = builder
          .given("test state")
          .uponReceiving("random port test interaction")
          .path("/")
          .method("GET")
          .willRespondWith()
          .status(200)
          .toFragment();
        return pactFragment;
    }

    @Test
    @PactVerification("test_provider")
    public void runTest() throws IOException {
        Map expectedResponse = new HashMap();
        assertEquals(new ConsumerClient("http://localhost:" + rule.getConfig().getPort()).getAsMap("/", ""), expectedResponse);
    }
}
