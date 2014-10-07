package au.com.dius.pact.consumer;

import au.com.dius.pact.model.PactFragment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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
                .closeObject()
            .closeArray();
        return builder
            .uponReceiving("java test interaction with a DSL body")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body(body)
            .toFragment();
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
            new ConsumerClient(url).get("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
