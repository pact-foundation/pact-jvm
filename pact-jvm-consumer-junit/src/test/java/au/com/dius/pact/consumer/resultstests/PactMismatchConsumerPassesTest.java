package au.com.dius.pact.consumer.resultstests;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class PactMismatchConsumerPassesTest extends ExpectedToFailBase {

    public PactMismatchConsumerPassesTest() {
        super(AssertionError.class);
    }

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "someotherheader");
        return builder
            .uponReceiving("PactVerifiedConsumerPassesTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
                .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"fred\"}")
            .toPact();
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
    protected void runTest(MockServer mockServer) throws IOException {
        new ConsumerClient(mockServer.getUrl()).getAsMap("/", "");
    }

    @Override
    protected void assertException(Throwable e) {
        assertThat(e.getMessage(),
            containsString("Expected header 'testreqheader' to have value 'someotherheader' but was 'testreqheadervalue'"));
    }
}
