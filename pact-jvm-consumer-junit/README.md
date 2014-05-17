pact-jvm-junit
===============

Bindings for the junit library

##Dependency

The library is available on maven central using:

group-id = `au.com.dius`

artifact-id = `pact-jvm-junit`

version-id = `2.0-RC1`

##Usage

To write a pact spec extend ConsumerPactTest

Here is an example:

```
import au.com.dius.pact.model.PactFragment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder.given("test state")
            .uponReceiving("java test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
                .body("{\"test\":true}")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\":true}").toFragment();
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
            assertEquals(new ConsumerClient(url).get("/"), "{\"responsetest\":true}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

