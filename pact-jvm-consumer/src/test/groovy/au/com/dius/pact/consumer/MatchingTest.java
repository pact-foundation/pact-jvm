package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactConfig;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MatchingTest {
    private static final VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;

    @Test
    public void testRegexpMatchingOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", "harry")
            .stringMatcher("position", "staff|contactor");

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", "harry");

        HashMap<String, String> expectedResponse = new HashMap<String, String>();
        expectedResponse.put("name", "harry");
        runTest(buildPactFragment(body, responseBody, "a test interaction that requires regex matching"),
            "{\"name\": \"Arnold\", \"position\": \"staff\"}", expectedResponse, "/hello");
    }

    @Test
    public void testMatchingByTypeOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringType("name")
                .booleanType("happy")
                .hexValue("hexCode")
                .id()
                .ipAddress("localAddress")
                .numberValue("age", 100)
                .numberType("ageAverage", 150.0)
                .integerType("age2", 200)
                .timestamp();

        PactDslJsonBody responseBody = new PactDslJsonBody();

        HashMap expectedResponse = new HashMap();
        runTest(buildPactFragment(body, responseBody, "a test interaction that requires type matching"),
            new JSONObject()
                .put("name", "Giggle and Hoot")
                .put("happy", true)
                .put("hexCode", "abcdef0123456789")
                .put("id", 1234567890)
                .put("localAddress", "192.168.0.1")
                .put("age", 100)
                .put("ageAverage", 150.0)
                .put("age2", 200)
                .put("timestamp", DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()))
                .toString(),
            expectedResponse, "/hello");
    }

    @Test
    public void testRegexpMatchingOnPath() {
        ConsumerPactBuilder.PactDslResponse fragment = ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving("a request to match on path")
            .matchPath("/hello/[0-9]{4}")
            .method("POST")
            .body("{}", ContentType.APPLICATION_JSON)
            .willRespondWith()
            .status(200);
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, "/hello/1234");
    }

    @Test
    public void testRegexpMatchingOnHeaders() {
        ConsumerPactBuilder.PactDslResponse fragment = ConsumerPactBuilder
                .consumer("test_consumer")
                .hasPactWith("test_provider")
                .uponReceiving("a request to match on headers")
                    .path("/hello")
                    .method("POST")
                    .matchHeader("testreqheader", "test.*value", "testreqheadervalue")
                .body("{}", ContentType.APPLICATION_JSON)
                .willRespondWith()
                .status(200)
                    .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234");
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, "/hello");
    }

    private void runTest(ConsumerPactBuilder.PactDslResponse pactFragment, final String body, final Map expectedResponse, final String path) {
        MockProviderConfig config = MockProviderConfig.createDefault(PactConfig.apply(2));
        VerificationResult result = pactFragment.toFragment().runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) {
                try {
                    Assert.assertEquals(new ConsumerClient(config.url()).post(path, body, ContentType.APPLICATION_JSON), expectedResponse);
                } catch (IOException e) {
                }
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        Assert.assertEquals(PACT_VERIFIED, result);
    }

    private ConsumerPactBuilder.PactDslResponse buildPactFragment(PactDslJsonBody body, PactDslJsonBody responseBody, String description) {
        return ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving(description)
                .path("/hello")
                .method("POST")
                .body(body)
            .willRespondWith()
                .status(200)
                .body(responseBody);
    }

}
