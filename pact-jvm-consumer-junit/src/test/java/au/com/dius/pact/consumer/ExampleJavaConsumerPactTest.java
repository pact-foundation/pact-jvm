package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("test state") // NOTE: Using provider states are optional, you can leave it out
            .uponReceiving("java test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body(
                    ConsumerPactBuilder.jsonBody()
                        .booleanValue("responsetest", true)
                )
            .uponReceiving("a second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
            .toFragment();
    }


    @Override
    protected String providerName() {
        return "test_provider";
    }

    @Override
    protected String consumerName() {
        return "test_consumer";
    }

    @Override
    protected void runTest(String url) {
        try {
            assertEquals(200, new ConsumerClient(url).options("/second"));
            assertEquals("{\"responsetest\":true}", new ConsumerClient(url).get("/"));
        } catch (Exception e) {
            // NOTE: if you want to see any pact failure, do not throw an exception here. This should be
            // fixed at some point (see Issue #40 https://github.com/DiUS/pact-jvm/issues/40)
            throw new RuntimeException(e);
        }
    }
}
