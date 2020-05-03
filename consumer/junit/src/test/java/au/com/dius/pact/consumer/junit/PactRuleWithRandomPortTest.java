package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
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
