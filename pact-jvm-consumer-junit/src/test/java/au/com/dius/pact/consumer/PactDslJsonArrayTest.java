package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.RequestResponsePact;

public class PactDslJsonArrayTest extends ConsumerPactTestMk2 {
    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
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
        RequestResponsePact pact = builder
          .uponReceiving("java test interaction with a DSL array body")
            .path("/")
            .method("GET")
          .willRespondWith()
            .status(200)
            .body(body)
          .toPact();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(pact,
            "$.body[0].id",
            "$.body[0].timestamp",
            "$.body[0].dob",
            "$.body[1].id",
            "$.body[1].timestamp",
            "$.body[1].dob"
        );

        return pact;
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
    protected void runTest(MockServer mockServer) {
        try {
            new ConsumerClient(mockServer.getUrl()).getAsList("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
