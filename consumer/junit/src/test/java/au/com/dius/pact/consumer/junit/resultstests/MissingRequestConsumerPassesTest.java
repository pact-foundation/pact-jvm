package au.com.dius.pact.consumer.junit.resultstests;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactMismatchesException;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MissingRequestConsumerPassesTest extends ExpectedToFailBase {

    public MissingRequestConsumerPassesTest() {
        super(PactMismatchesException.class);
    }

    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
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
    }

    @Override
    protected void assertException(Throwable e) {
        assertThat(e.getMessage(),
            containsString("The following requests were not received:\n" +
              "\tmethod: OPTIONS\n" +
              "\tpath: /second\n" +
              "\tquery: {}\n" +
              "\theaders: {testreqheader=[testreqheadervalue]}\n" +
              "\tmatchers: MatchingRules(rules={path=Category(name=path, matchingRules={}), header=Category(name=header, matchingRules={}), body=Category(name=body, matchingRules={})})\n" +
              "\tgenerators: Generators(categories={})\n" +
              "\tbody: EMPTY"));

    }
}
