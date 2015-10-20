package au.com.dius.pact.consumer.examples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import au.com.dius.pact.consumer.ConsumerPactTest;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import org.apache.http.client.fluent.Request;

import static org.junit.Assert.assertEquals;

public class StatusServiceConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
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
    protected void runTest(String baseUrl) throws IOException {
        StatusServiceClient statusServiceClient = new StatusServiceClient(baseUrl);

        String currentQuestionnairePage = statusServiceClient.getCurrentQuestionnairePage(null);

        assertEquals(currentQuestionnairePage, "my_home_1");
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
