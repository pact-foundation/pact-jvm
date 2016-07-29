package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.MockProviderConfig$;
import au.com.dius.pact.model.PactConfig;
import au.com.dius.pact.model.PactSpecVersion;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MatchingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingTest.class);

    private static final VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;
    private static final String HARRY = "harry";
    private static final String HELLO = "/hello";
    private static final String TEST_CONSUMER = "test_consumer";
    private static final String TEST_PROVIDER = "test_provider";

    @Test
    public void testRegexpMatchingOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", HARRY)
            .stringMatcher("position", "staff|contactor");

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
            .status(200);
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
                    .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234");
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

    private void runTest(PactDslResponse pactFragment, final String body, final Map expectedResponse, final String path) {
        MockProviderConfig config = MockProviderConfig$.MODULE$.createDefault(new PactConfig(PactSpecVersion.V2));
        VerificationResult result = pactFragment.toFragment().runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) throws IOException {
                try {
                    Assert.assertEquals(new ConsumerClient(config.url()).post(path, body, ContentType.APPLICATION_JSON), expectedResponse);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                    throw e;
                }
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        Assert.assertEquals(PACT_VERIFIED, result);
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
