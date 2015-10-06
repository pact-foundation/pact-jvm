package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MimeTypeTest {

    private static final VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    @Test
    public void testMatchingJson() {
        String body = new PactDslJsonBody()
            .object("person")
                .stringValue("name", "fred")
                .numberValue("age", 100)
            .closeObject()
            .toString();

        String responseBody = "{\"status\": \"OK\"}";

        runTest(buildPactFragment(body, responseBody, "a test interaction with json", ContentType.APPLICATION_JSON),
            body, responseBody, ContentType.APPLICATION_JSON);
    }

    @Test
    public void testMatchingText() {
        String body = "Define a pact between service consumers and providers, enabling \"consumer driven contract\" testing.\n" +
            "\nPact provides an RSpec DSL for service consumers to define the HTTP requests they will make to a service" +
            " provider and the HTTP responses they expect back. These expectations are used in the consumers specs " +
            "to provide a mock service provider. The interactions are recorded, and played back in the service " +
            "provider specs to ensure the service provider actually does provide the response the consumer expects.";

        String responseBody = "status=OK";

        runTest(buildPactFragment(body, responseBody, "a test interaction with text", ContentType.TEXT_PLAIN),
            body, responseBody, ContentType.TEXT_PLAIN);
    }

    @Test
    public void testMatchingXml() {
        String body = "<?xml version=\"1.0\"?>\n<someXml></someXml>";

        String responseBody = "<status>OK</status>";

        runTest(buildPactFragment(body, responseBody, "a test interaction with xml", ContentType.APPLICATION_XML),
            body, responseBody, ContentType.APPLICATION_XML);
    }

    private void runTest(PactFragment pactFragment, final String body, final String expectedResponse, final ContentType mimeType) {
        MockProviderConfig config = MockProviderConfig.createDefault(PactConfig.apply(PactSpecVersion.V2));
        VerificationResult result = pactFragment.runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) {
                try {
                    assertEquals(new ConsumerClient(config.url()).postBody("/hello", body, mimeType), expectedResponse);
                } catch (IOException e) {}
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        Assert.assertEquals(PACT_VERIFIED, result);
    }

    private PactFragment buildPactFragment(String body, String responseBody, String description, ContentType contentType) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", contentType.toString());
        return ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving(description)
                .path("/hello")
                .method("POST")
                .body(body)
                .headers(headers)
            .willRespondWith()
                .status(200)
                .body(responseBody)
                .headers(headers)
                .toFragment();
    }

}
