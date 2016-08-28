package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;

public class PactDslJsonArrayTest extends ConsumerPactTest {
    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonArray()
          .object()
            .id()
            .stringValue("name", "Rogger the Dogger")
            .timestamp()
            .date("dob", "MM/dd/yyyy")
          .closeObject()
          .object()
            .id()
            .stringValue("name", "Cat in the Hat")
            .timestamp()
            .date("dob", "MM/dd/yyyy")
          .closeObject();
        PactFragment fragment = builder
          .uponReceiving("java test interaction with a DSL array body")
            .path("/")
            .method("GET")
          .willRespondWith()
            .status(200)
            .body(body)
          .toFragment();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(fragment, "body",
            "$[0].id",
            "$[0].timestamp",
            "$[0].dob",
            "$[1].id",
            "$[1].timestamp",
            "$[1].dob"
        );

        return fragment;
    }

    @Override
    protected String providerName() {
        return "test_provider_array";
    }

    @Override
    protected String consumerName() {
        return "test_consumer_array";
    }

    @Override
    protected void runTest(String url) {
        try {
            new ConsumerClient(url).getAsList("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
