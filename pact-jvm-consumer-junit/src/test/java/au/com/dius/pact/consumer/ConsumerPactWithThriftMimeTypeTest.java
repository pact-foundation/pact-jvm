package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;
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
    private static final String APPLICATION_X_THRIFT_JSON = "application/x-thrift+json";

    @Rule
    public PactProviderRuleMk2 provider = new PactProviderRuleMk2("test_provider", "localhost", 8080, this);

    @Pact(provider="test_provider", consumer="test_consumer")
    public RequestResponsePact createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", APPLICATION_X_THRIFT_JSON);

        return builder
            .given("test state")
            .uponReceiving("ConsumerPactWithThriftMimeTypeTest test interaction")
                .path("/persons/429605785802342400")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .body(BODY, "application/x-thrift+json")
            .toPact();
    }

    @Test
    @PactVerification("test_provider")
    public void runTest() throws IOException {
        assertEquals(Request.Get("http://localhost:8080/persons/429605785802342400")
            .addHeader("Accept", "application/x-thrift+json")
            .execute().returnContent().getType().getMimeType(), "application/x-thrift+json");
    }
}
