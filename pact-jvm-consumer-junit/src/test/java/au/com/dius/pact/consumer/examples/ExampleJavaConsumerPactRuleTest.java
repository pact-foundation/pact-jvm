package au.com.dius.pact.consumer.examples;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.model.PactFragment;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactRuleTest {

    @Rule
    public PactProviderRule provider = new PactProviderRule("test_provider", this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("test state")
            .uponReceiving("ExampleJavaConsumerPactRuleTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .given("test state")
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
    @PactVerification("test_provider")
    public void runTest() throws IOException {
        Assert.assertEquals(new ConsumerClient(provider.getConfig().url()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(provider.getConfig().url()).getAsMap("/", ""), expectedResponse);
    }
}
