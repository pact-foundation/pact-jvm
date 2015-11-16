package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactFragment;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PactDslJsonBodyTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
            .id()
            .object("2")
                .id()
                .stringValue("test", null)
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

        MatcherTestUtils.assertResponseMatcherKeysEqualTo(fragment,
            "$.body.id",
            "$.body['2'].id",
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
        Map response;
        try {
            response = new ConsumerClient(url).getAsMap("/", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> object2 = (Map<String, Object>) response.get("2");
        assertThat(object2, hasKey("test"));
        assertThat(object2.get("test"), is(nullValue()));
    }
}
