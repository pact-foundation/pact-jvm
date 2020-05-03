package au.com.dius.pact.consumer.junit;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.RequestResponsePact;

public class PactDslJsonArrayTest extends ConsumerPactTest {
    @Override
    protected RequestResponsePact createPact(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonArray()
          .includesStr("test")
          .equalsTo("Test")
          .object()
            .id()
            .stringValue("name", "Rogger the Dogger")
            .includesStr("v1", "test")
            .timestamp()
            .date("dob", "MM/dd/yyyy")
          .closeObject()
          .object()
            .id()
            .stringValue("name", "Cat in the Hat")
            .timestamp()
            .date("dob", "MM/dd/yyyy")
            .array("things")
                .valueFromProviderState("thingName", "Thing 1")
            .closeArray()
          .closeObject();
        RequestResponsePact pact = builder
          .uponReceiving("java test interaction with a DSL array body")
            .path("/")
            .method("GET")
          .willRespondWith()
            .status(200)
            .body(body)
          .toPact();

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(pact, "body",
            "$[0]",
            "$[1]",
            "$[2].id",
            "$[2].timestamp",
            "$[2].dob",
            "$[2].v1",
            "$[3].id",
            "$[3].timestamp",
            "$[3].dob",
            "$[3].things[0]"
        );

        MatcherTestUtils.assertResponseGeneratorKeysEqualTo(pact, "body",
            "$[2].id",
            "$[2].timestamp",
            "$[2].dob",
            "$[3].id",
            "$[3].timestamp",
            "$[3].dob",
            "$[3].things[0]");

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
    protected void runTest(MockServer mockServer, PactTestExecutionContext context) {
        try {
            new ConsumerClient(mockServer.getUrl()).getAsList("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
