package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.ConsumerPactBuilder.PactDslWithProvider.PactDslWithState;
import au.com.dius.pact.model.PactFragment;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactRuleWithRandomPortTest {

    @Rule
    public PactRule rule = new PactRule(this);

    @Pact(state="test state", provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithState builder) {
        return builder
            .uponReceiving("random port test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"ok\": true}")
            .toFragment();
    }

    @Test
    @PactVerification("test state")
    public void runTest() throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("ok", true);
        assertEquals(new ConsumerClient("http://localhost:" + rule.config.port()).getAsMap("/"), expectedResponse);
    }
}
