package au.com.dius.pact.consumer.pactproviderrule;

import au.com.dius.pact.consumer.ConsumerClient;
import au.com.dius.pact.consumer.ConsumerPactBuilder;
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

public class PactProviderWithMultipleFragmentsTest {

    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", this);

    @Pact(consumer="test_consumer")
    public PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
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

    @Pact(consumer="test_consumer")
    public PactFragment createFragment2(ConsumerPactBuilder.PactDslWithProvider builder) {
        return builder
                .given("good state")
                .uponReceiving("PactProviderTest test interaction 2")
                    .path("/")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body("{\"responsetest\": true, \"name\": \"harry\"}")
                .toFragment();
    }

    @Test
    @PactVerification(value = "test_provider", fragment = "createFragment")
    public void runTestWithFragment1() throws IOException {
        Assert.assertEquals(new ConsumerClient(mockTestProvider.getConfig().url()).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getConfig().url()).getAsMap("/"), expectedResponse);
    }

    @Test
    @PactVerification(value = "test_provider", fragment = "createFragment2")
    public void runTestWithFragment2() throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(mockTestProvider.getConfig().url()).getAsMap("/"), expectedResponse);
    }
}
