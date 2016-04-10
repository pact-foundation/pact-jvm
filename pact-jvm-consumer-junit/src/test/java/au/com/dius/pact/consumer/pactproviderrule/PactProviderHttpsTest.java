package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.consumer.exampleclients.ConsumerHttpsClient;
import au.com.dius.pact.model.PactConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactProviderHttpsTest {

    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", "localhost", 8443, true,
      PactConfig.apply(PactSpecVersion.V2), this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("good state")
            .uponReceiving("PactProviderTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .uponReceiving("PactProviderTest second test interaction")
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
    @PactVerification(value = "test_provider")
    public void runTest() throws IOException {
        Assert.assertEquals(new ConsumerHttpsClient(mockTestProvider.getConfig().url()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerHttpsClient(mockTestProvider.getConfig().url()).getAsMap("/", ""), expectedResponse);
    }

    @Test(expected = AssertionError.class)
    @PactVerification("test_provider")
    public void runTestWithUserCodeFailure() throws IOException {
        Assert.assertEquals(new ConsumerHttpsClient(mockTestProvider.getConfig().url()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerHttpsClient(mockTestProvider.getConfig().url()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerification(value = "test_provider", expectMismatch = true)
    public void runTestWithPactError() throws IOException {
        Assert.assertEquals(new ConsumerHttpsClient(mockTestProvider.getConfig().url()).options("/second"), 200);
    }
}
