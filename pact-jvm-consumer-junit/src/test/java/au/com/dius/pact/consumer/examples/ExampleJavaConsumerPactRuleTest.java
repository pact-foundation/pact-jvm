package au.com.dius.pact.consumer.examples;

import au.com.dius.pact.consumer.ConsumerClient;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.consumer.ConsumerPactBuilder.PactDslWithProvider.PactDslWithState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
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
            .uponReceiving("ExampleJavaConsumerPactRuleTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("ExampleJavaConsumerPactRuleTest second test interaction")
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
    public void runTest() throws IOException {
        Assert.assertEquals(new ConsumerClient("http://localhost:8080").options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient("http://localhost:8080").getAsMap("/"), expectedResponse);
    }
}
