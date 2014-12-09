package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.consumer.ConsumerPactBuilder.PactDslWithProvider.PactDslWithState;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactRuleTest {

    @Rule
    public PactRule rule = new PactRule("localhost", 8080, this);

    @Pact(state="test state", provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithState builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("ExampleJavaConsumerPactTest second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
            .toFragment();
    }



    @Test
    @PactVerification("test state")
    public void runTest() {
        try {
            assertEquals(new ConsumerClient("http://localhost:8080").options("/second"), 200);
            Map expectedResponse = new HashMap();
            expectedResponse.put("responsetest", true);
            expectedResponse.put("name", "harry");
            assertEquals(new ConsumerClient("http://localhost:8080").get("/"), expectedResponse);
        } catch (Exception e) {
            // NOTE: if you want to see any pact failure, do not throw an exception here. This should be
            // fixed at some point (see Issue #40 https://github.com/DiUS/pact-jvm/issues/40)
            throw new RuntimeException(e);
        }
    }
}
