package au.com.dius.pact.consumer.examples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.ConsumerPactTest;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;

import static org.junit.Assert.assertEquals;

public class StatusServiceConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .uponReceiving("status")
            .path("/status")
            .method("GET")
            .headers(headers)
            .willRespondWith()
            .status(200)
            .headers(headers)
            .body("{\"responsetest\":true}").toFragment();
    }

    @Override
    protected String providerName() {
        return "provider";
    }

    @Override
    protected String consumerName() {
        return "consumer";
    }

    @Override
    protected void runTest(String baseUrl) {
        try {
            StatusServiceClient statusServiceClient = new StatusServiceClient(baseUrl);

            String currentQuestionnairePage = statusServiceClient.getCurrentQuestionnairePage(null);

            assertEquals(currentQuestionnairePage, "my_home_1");
        } catch (Exception e) {
            // NOTE: if you want to see any pact failure, do not throw an exception here. This should be
            // fixed at some point (see Issue #40 https://github.com/DiUS/pact-jvm/issues/40)
            throw new RuntimeException(e);
        }
    }

    private class StatusServiceClient {
        private String baseUrl;

        public StatusServiceClient(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getCurrentQuestionnairePage(Object page) throws IOException {
            String response = Request.Get(baseUrl + "/status")
                .addHeader("testreqheader", "testreqheadervalue")
                .execute().returnContent().asString();

            return "my_home_1";
        }
    }
}
