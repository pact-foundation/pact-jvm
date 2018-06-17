package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactRuleWithRandomPortTest {

    @Rule
    public PactProviderRuleMk2 rule = new PactProviderRuleMk2("test_provider", this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        RequestResponsePact pact = builder
          .given("test state")
          .uponReceiving("random port test interaction")
          .path("/")
          .method("GET")
          .willRespondWith()
          .status(200)
          .toPact();
        return pact;
    }

    @Test
    @PactVerification("test_provider")
    public void runTest() throws IOException {
        Map expectedResponse = new HashMap();
        assertEquals(new ConsumerClient("http://localhost:" + rule.getPort()).getAsMap("/", ""), expectedResponse);
    }
}
