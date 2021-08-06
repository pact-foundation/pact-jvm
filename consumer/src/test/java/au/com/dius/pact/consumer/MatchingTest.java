package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.PactSpecVersion;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MatchingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingTest.class);

    private static final String HARRY = "harry";
    private static final String HELLO = "/hello";
    private static final String TEST_CONSUMER = "test_consumer";
    private static final String TEST_PROVIDER = "test_provider";

    @Test
    public void testRegexpMatchingOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", HARRY)
            .stringMatcher("position", "staff|contractor", "staff");

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", HARRY);

        HashMap<String, String> expectedResponse = new HashMap<String, String>();
        expectedResponse.put("name", HARRY);
        runTest(buildPactFragment(body, responseBody, "a test interaction that requires regex matching"),
            "{\"name\": \"Arnold\", \"position\": \"staff\"}", expectedResponse, HELLO);
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
            .datetime("timestamp");

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
            expectedResponse, HELLO);
    }

    @Test
    public void testRegexpMatchingOnPath() {
        PactDslResponse fragment = ConsumerPactBuilder
            .consumer(TEST_CONSUMER)
            .hasPactWith(TEST_PROVIDER)
            .uponReceiving("a request to match on path")
            .matchPath("/hello/[0-9]{4}")
            .method("POST")
            .body("{}", ContentType.APPLICATION_JSON)
            .willRespondWith()
            .status(200)
            .body("", "text/plain");
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, "/hello/1234");
    }

    @Test
    public void testRegexpMatchingOnHeaders() {
        PactDslResponse fragment = ConsumerPactBuilder
                .consumer(TEST_CONSUMER)
                .hasPactWith(TEST_PROVIDER)
                .uponReceiving("a request to match on headers")
                    .path(HELLO)
                    .method("POST")
                    .matchHeader("testreqheader", "test.*value", "testreqheadervalue")
                .body("{}", ContentType.APPLICATION_JSON)
                .willRespondWith()
                .status(200)
                    .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234")
                    .body("", "text/plain");
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, HELLO);
    }

    @Test
    public void testRegexCharClassStringGenerator() {
        PactDslJsonBody numeric = new PactDslJsonBody()
                .stringMatcher("x", "\\d+");
        String val = new JSONObject(numeric.getBody().toString()).getString("x");
        Assert.assertTrue("'" + val + "' is not a number", NumberUtils.isDigits(val));

        PactDslJsonBody numericWithLimitedRep = new PactDslJsonBody()
                .stringMatcher("x", "\\d{9}");
        val = new JSONObject(numericWithLimitedRep.getBody().toString()).getString("x");
        Assert.assertTrue("'" + val + "' is not a number", NumberUtils.isDigits(val));
    }

    @Test
    public void testRegexpMatchingOnQueryParameters() {
        PactDslResponse fragment = ConsumerPactBuilder
          .consumer(TEST_CONSUMER)
          .hasPactWith(TEST_PROVIDER)
          .uponReceiving("a request to match on query parameters")
          .path(HELLO)
          .method("POST")
          .matchQuery("a", "\\d+")
          .matchQuery("b", "\\d+")
          .matchQuery("c", "[A-Z]")
          .body("{}", ContentType.APPLICATION_JSON)
          .willRespondWith()
          .status(200)
          .body("", "text/plain");
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, HELLO + "?a=100&b=200&c=X");
    }

    private void runTest(PactDslResponse pactFragment, final String body, final Map expectedResponse, final String path) {
        MockProviderConfig config = MockProviderConfig.createDefault(PactSpecVersion.V3);
        PactVerificationResult result = runConsumerTest(pactFragment.toPact(), config, (mockServer, context) -> {
            try {
                Assert.assertEquals(expectedResponse, new ConsumerClient(mockServer.getUrl()).post(path, body, ContentType.APPLICATION_JSON));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
            }
            return true;
        });

        if (result instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error)result).getError());
        }

        assertThat(result, is(instanceOf(PactVerificationResult.Ok.class)));
    }

    private PactDslResponse buildPactFragment(PactDslJsonBody body, PactDslJsonBody responseBody, String description) {
        return ConsumerPactBuilder
            .consumer(TEST_CONSUMER)
            .hasPactWith(TEST_PROVIDER)
            .uponReceiving(description)
                .path(HELLO)
                .method("POST")
                .body(body)
            .willRespondWith()
                .status(200)
                .body(responseBody);
    }

}
