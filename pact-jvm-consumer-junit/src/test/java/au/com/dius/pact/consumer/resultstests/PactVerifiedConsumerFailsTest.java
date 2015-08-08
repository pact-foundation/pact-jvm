package au.com.dius.pact.consumer.resultstests;

import au.com.dius.pact.consumer.ConsumerClient;
import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.model.PactFragment;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PactVerifiedConsumerFailsTest extends ExpectedToFailBase {

    public PactVerifiedConsumerFailsTest() {
        super(AssertionError.class);
    }

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        return builder
            .uponReceiving("PactVerifiedConsumerPassesTest test interaction")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .toFragment();
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
    protected void runTest(String url) throws IOException {
        Map<String, Object> expectedResponse = new HashMap<String, Object>();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "fred");
        assertEquals(new ConsumerClient(url).getAsMap("/"), expectedResponse);
    }

    @Override
    protected void assertException(Throwable e) {
        if (SystemUtils.IS_JAVA_1_8) {
            assertThat(e.getMessage(),
                containsString("expected:<{responsetest=true, name=harry}> but was:<{responsetest=true, name=fred}>"));
        } else {
            assertThat(e.getMessage(),
                containsString("expected:<{name=harry, responsetest=true}> but was:<{name=fred, responsetest=true}>"));
        }
    }
}
