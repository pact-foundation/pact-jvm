package au.com.dius.pact.consumer.v3;

import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class V3PactProviderTest {

    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", PactSpecVersion.V3, this);

    @Pact(provider="test_provider", consumer="v3_test_consumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        return builder
            .given("good state")
            .uponReceiving("V3 PactProviderTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"version\": \"v3\"}")
            .toPact();
    }

    @Test
    @PactVerification
    public void runTest() throws IOException {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("version", "v3");
        assertEquals(new ConsumerClient(mockTestProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

}
