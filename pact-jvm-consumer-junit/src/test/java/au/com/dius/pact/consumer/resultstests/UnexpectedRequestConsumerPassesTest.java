package au.com.dius.pact.consumer.resultstests;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactMismatchesException;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.RequestResponsePact;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class UnexpectedRequestConsumerPassesTest extends ExpectedToFailBase {

    public UnexpectedRequestConsumerPassesTest() {
        super(PactMismatchesException.class);
    }

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");
        return builder
            .uponReceiving("UnexpectedRequestConsumerPassesTest test interaction")
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
    protected void runTest(MockServer mockServer, PactTestExecutionContext context) throws IOException {
        Map<String, Object> expectedResponse = new HashMap<String, Object>();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        ConsumerClient consumerClient = new ConsumerClient(mockServer.getUrl());
        assertEquals(consumerClient.getAsMap("/", ""), expectedResponse);
        consumerClient.options("/options");
    }


    @Override
    protected void assertException(Throwable e) {
        assertThat(e.getMessage(),
            containsString("The following mismatched requests occurred:\n" +
              "Unexpected Request:\n" +
              "\tmethod: OPTIONS\n" +
              "\tpath: /options"));

    }
}
