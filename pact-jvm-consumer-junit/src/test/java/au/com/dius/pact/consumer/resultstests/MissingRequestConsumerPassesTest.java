package au.com.dius.pact.consumer.resultstests;

import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.PactMismatchException;
import au.com.dius.pact.model.PactFragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MissingRequestConsumerPassesTest extends ExpectedToFailBase {

    public MissingRequestConsumerPassesTest() {
        super(PactMismatchException.class);
    }

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");
        return builder
            .uponReceiving("MissingRequestConsumerPassesTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
                .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"fred\"}")
            .uponReceiving("MissingRequestConsumerPassesTest second test interaction")
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


    @Override
    protected String providerName() {
        return "resultstests_provider";
    }

    @Override
    protected String consumerName() {
        return "resultstests_consumer";
    }

    @Override
    protected void runTest(String url) throws IOException {
        Map<String, Object> expectedResponse = new HashMap<String, Object>();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        ConsumerClient consumerClient = new ConsumerClient(url);
        assertEquals(consumerClient.getAsMap("/", ""), expectedResponse);
    }

    @Override
    protected void assertException(Throwable e) {
        assertThat(e.getMessage(),
            containsString("The following requests were not received:\n" +
                "Interaction: MissingRequestConsumerPassesTest second test interaction\n" +
                "\tin state None\n" +
                "request:\n" +
                "\tmethod: OPTIONS\n" +
                "\tpath: /second\n"));

    }
}
