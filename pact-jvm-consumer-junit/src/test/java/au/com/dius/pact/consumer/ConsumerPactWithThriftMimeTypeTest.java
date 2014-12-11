package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.ConsumerPactBuilder.PactDslWithProvider.PactDslWithState;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConsumerPactWithThriftMimeTypeTest {

    private static final String BODY = "{\"1\":{\"str\":\"429605785802342400\"},\"2\":{\"str\":\"ggloin\"},\"3\":{\"str\":\"1859\"}," +
        "\"4\":{\"rec\":{\"1\":{\"str\":\"Gloin\"},\"2\":{\"str\":\"Gimli\"}}},\"5\":{\"rec\":{\"1\":" +
        "{\"str\":\"City\"},\"2\":{\"str\":\"United States\"},\"3\":{\"str\":\"UTC+02:00 Eastern Europe\"" +
        "}}},\"6\":{\"str\":\"http://xxx.com/Users:ggloin:PortraitUrl?AWSAccessKeyId=aaaa&Expires=" +
        "2147483647&Signature=aaaa4%2B4%3D\"},\"7\":{\"str\":\"2014-02-01T13:22:52.585Z\"},\"8\":" +
        "{\"str\":\"1\"}}";

    @Rule
    public PactRule rule = new PactRule("localhost", 8080, this);

    @Pact(state="test state", provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithState builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/x-thrift+json");

        return builder
            .uponReceiving("ConsumerPactWithThriftMimeTypeTest test interaction")
                .path("/persons/429605785802342400")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .body(BODY, "application/x-thrift+json")
            .toFragment();
    }

    @Test
    @PactVerification("test state")
    public void runTest() throws IOException {
        assertEquals(Request.Get("http://localhost:8080/persons/429605785802342400")
            .addHeader("Accept", "application/x-thrift+json")
            .execute().returnContent().getType().getMimeType(), "application/x-thrift+json");
    }
}
