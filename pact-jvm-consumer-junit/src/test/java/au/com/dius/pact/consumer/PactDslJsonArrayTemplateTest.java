package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;

public class PactDslJsonArrayTemplateTest extends ConsumerPactTest {
    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        DslPart personTemplate = new PactDslJsonBody()
                .id()
                .stringType("name")
                .date("dob");

        DslPart body = new PactDslJsonArray()
                .template(personTemplate, 3);

        PactFragment fragment = builder
                .uponReceiving("java test interaction with a DSL array body with templates")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(body)
                .toFragment();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(fragment, "body",
            "$[0].id",
            "$[0].name",
            "$[0].dob",
            "$[1].id",
            "$[1].name",
            "$[1].dob",
            "$[2].id",
            "$[2].name",
            "$[2].dob"
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
