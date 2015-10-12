package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;

import java.io.IOException;

public class QueryParameterEncodingTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        PactFragment fragment = builder
                .uponReceiving("java test interaction with a query string")
                .path("/some path")
                .method("GET")
                .query("datetime=2011-12-03T10:15:30+01:00")
                .willRespondWith()
                .status(200)
                .body("{}")
                .toFragment();

        return fragment;
    }

    @Override
    protected String providerName() {
        return "test_provider";
    }

    @Override
    protected String consumerName() {
        return "test_consumer";
    }

    @Override
    protected void runTest(String url) throws IOException {
        new ConsumerClient(url).getAsMap("/some path", "datetime=2011-12-03T10:15:30+01:00");
    }
}
