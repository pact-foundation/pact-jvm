pact-jvm-consumer-junit
=======================

Provides a DSL and a base test class for use with Junit to build consumer tests.

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-junit_2.11`
* version-id = `2.0.8`

##Usage

### Using the base ConsumerPactTest

To write a pact spec extend ConsumerPactTest. This base class defines the following four methods which must be
overridden in your test class.

* *providerName:* Returns the name of the API provider that Pact will mock
* *consumerName:* Returns the name of the API consumer that we are testing.
* *createFragment:* Returns the PactFrament containing the interactions that the test setup using the
  ConsumerPactBuilder DSL
* *runTest:* The actual test run. It receives the URL to the mock server as a parameter.

Here is an example:

```java
import au.com.dius.pact.model.PactFragment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(ConsumerPactBuilder.PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("test state") // NOTE: Using provider states are optional, you can leave it out
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
            // NOTE: if you want to see any pact failure, do not throw an exception here. This should be
            // fixed at some point (see Issue #40 https://github.com/DiUS/pact-jvm/issues/40)
            throw new RuntimeException(e);
        }
    }
}
```

### Using the Pact DSL directly

Sometimes it is not convenient to use the ConsumerPactTest as it only allows one test per test class. The DSL can be
 used directly in this case.

Example:

```java
import au.com.dius.pact.model.MockProviderConfig;
import au.com.dius.pact.model.PactFragment;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PactTest {

    @Test
    public void testPact() {
        PactFragment pactFragment = ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving("a test interaction")
                .path("/hello")
                .method("POST")
                .body("{\"name\": \"harry\"}")
            .willRespondWith()
                .status(200)
                .body("{\"hello\": \"harry\"}")
                .toFragment();

        MockProviderConfig config = MockProviderConfig.createDefault();
        VerificationResult result = pactFragment.runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) {
                Map expectedResponse = new HashMap();
                expectedResponse.put("hello", "harry");
                try {
                    assertEquals(new ConsumerClient(config.url()).post("/hello", "{\"name\": \"harry\"}"),
                            expectedResponse);
                } catch (IOException e) {}
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        assertEquals(ConsumerPactTest.PACT_VERIFIED, result);
    }

}

```

### The Pact JUnit DSL

The DSL has the following pattern:

```java
.consumer("test_consumer")
.hasPactWith("test_provider")
.given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .uponReceiving("a second test interaction")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .
    .
    .
.toFragment()
```

You can define as many interactions as required. Each interaction starts with `uponReceiving` followed by `willRespondWith`.
The test state setup with `given` is a mechanism to describe what the state of the provider should be in before the provider
is verified. It is only recorded in the consumer tests and used by the provider verification tasks.

## Debugging pact failures

When the test runs, Pact will start a mock provider that will listen for requests and match them against the expectations
you setup in `createFragment`. If the request does not match, it will return a 500 error response.

Each request received and the generated response is logged using [SLF4J](http://www.slf4j.org/). Just enable debug level
logging for au.com.dius.pact.consumer.UnfilteredMockProvider. Most failures tend to be mismatched headers or bodies.
