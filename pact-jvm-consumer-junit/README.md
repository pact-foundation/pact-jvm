pact-jvm-consumer-junit
=======================

Provides a DSL and a base test class for use with Junit to build consumer tests.

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-junit_2.11`
* version-id = `2.1.x`

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
            .uponReceiving("a request for something")
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
        return "Some Provider";
    }

    @Override
    protected String consumerName() {
        return "Some Consumer";
    }

    @Override
    protected void runTest(String url) {
        try {
            assertEquals(new ProviderClient(url).getSomething(), "{\"responsetest\":true}");
        } catch (Exception e) {
            // NOTE: if you want to see any pact failure, do not throw an exception here. This should be
            // fixed at some point (see Issue #40 https://github.com/DiUS/pact-jvm/issues/40)
            throw new RuntimeException(e);
        }
    }
}
```

### Using the Pact JUnit Rule

Thanks to [@warmuuh](https://github.com/warmuuh) we have a JUnit rule that simplifies running Pact consumer tests. To use it, create a test class
and then add the rule:

#### 1. Add the Pact Rule to your test class.

```java
    @Rule
    public PactRule rule = new PactRule("localhost", 8080, this);
```

#### 2. Annotate a method with Pact that returns a pact fragment

```java
    @Pact(state="test state", provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithState builder) {
        return builder
            .uponReceiving("ExampleJavaConsumerPactRuleTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true}")
            .toFragment();
    }
```

#### 3. Annotate your test method with PactVerification to have it run in the context of a mock server setup with the appropriate pact from step 2

```java
    @Test
    @PactVerification("test state")
    public void runTest() {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        assertEquals(new ConsumerClient("http://localhost:8080").get("/"), expectedResponse);
    }
```

For an example, have a look at [ExampleJavaConsumerPactRuleTest](src/test/java/au/com/dius/pact/consumer/examples/ExampleJavaConsumerPactRuleTest.java)

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
            .consumer("Some Consumer")
            .hasPactWith("Some Provider")
            .uponReceiving("a request to say Hello")
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
                    assertEquals(new ProviderClient(config.url()).hello("{\"name\": \"harry\"}"),
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
.consumer("Some Consumer")
.hasPactWith("Some Provider")
.given("a certain state on the provider")
    .uponReceiving("a request for something")
        .path("/hello")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
    .uponReceiving("another request for something")
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

### Building JSON bodies with PactDslJsonBody DSL

The body method of the ConsumerPactBuilder can accept a PactDslJsonBody, which can construct a JSON body as well as
define regex and type matchers.

For example:

```java
PactDslJsonBody body = new PactDslJsonBody()
    .stringType("name")
    .booleanType("happy")
    .hexValue("hexCode")
    .id()
    .ipAddress("localAddress")
    .numberValue("age", 100)
    .timestamp();
```

### Matching on paths (version 2.1.5+)

You can use regular expressions to match incoming requests. The DSL has a `matchPath` method for this. You can provide
a real path as a second value to use when generating requests, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .matchPath("/transaction/[0-9]+") // or .matchPath("/transaction/[0-9]+", "/transaction/1234567890")
        .method("POST")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
```

##

## Debugging pact failures

When the test runs, Pact will start a mock provider that will listen for requests and match them against the expectations
you setup in `createFragment`. If the request does not match, it will return a 500 error response.

Each request received and the generated response is logged using [SLF4J](http://www.slf4j.org/). Just enable debug level
logging for au.com.dius.pact.consumer.UnfilteredMockProvider. Most failures tend to be mismatched headers or bodies.

## Changing the directory pact files are written to (2.1.9+)

By default, pact files are written to `target/pacts`, but this can be overwritten with the `pact.rootDir` system property.
This property needs to be set on the test JVM as most build tools will fork a new JVM to run the tests.

For Gradle, add this to your build.gradle:

```groovy
test {
    systemProperties['pact.rootDir'] = "$buildDir/pacts"
}
```

For maven, use the systemPropertyVariables configuration:

```xml
<project>
  [...]
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.18</version>
        <configuration>
          <systemPropertyVariables>
            <pact.rootDir>some/other/directory</pact.rootDir>
            <buildDirectory>${project.build.directory}</buildDirectory>
            [...]
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
  [...]
</project>
```

For SBT:

```scala
fork in Test := true,
javaOptions in Test := Seq("-Dpact.rootDir=some/other/directory")
```
