package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

import java.util.HashSet;
import java.util.Set;

public class PactDslJsonBodyTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
            .id()
            .object("obj")
                .id()
                .stringValue("test", "A Test String")
            .closeObject()
            .array("numbers")
                .id()
                .number(100)
                .numberValue(101)
                .hexValue()
                .object()
                    .id()
                    .stringValue("name", "Rogger the Dogger")
                    .timestamp()
                    .date("dob", "MM/dd/yyyy")
                .closeObject()
            .closeArray();
        PactFragment fragment = builder
                .uponReceiving("java test interaction with a DSL body")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(body)
                .toFragment();

        MatcherTestUtils.assertResponseMatchersEqualTo(fragment,
                "$.body.id",
                "$.body.obj.id",
                "$.body.numbers[0]",
                "$.body.numbers[3]",
                "$.body.numbers[4].id",
                "$.body.numbers[4].timestamp",
                "$.body.numbers[4].dob");

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
    protected void runTest(String url) {
        try {
            new ConsumerClient(url).getAsMap("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
