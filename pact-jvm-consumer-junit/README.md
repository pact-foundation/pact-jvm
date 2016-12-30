pact-jvm-consumer-junit
=======================

Provides a DSL and a base test class for use with Junit to build consumer tests.

##Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-junit_2.11`
* version-id = `3.2.x`

##Usage

### Using the base ConsumerPactTest

To write a pact spec extend ConsumerPactTest. This base class defines the following four methods which must be
overridden in your test class.

* *providerName:* Returns the name of the API provider that Pact will mock
* *consumerName:* Returns the name of the API consumer that we are testing.
* *createFragment:* Returns the PactFragment containing the interactions that the test setup using the
  ConsumerPactBuilder DSL
* *runTest:* The actual test run. It receives the URL to the mock server as a parameter.

Here is an example:

```java
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.consumer.ConsumerPactTest;
import au.com.dius.pact.model.PactFragment;
import org.junit.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExampleJavaConsumerPactTest extends ConsumerPactTest {

    @Override
    protected PactFragment createFragment(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("testreqheader", "testreqheadervalue");

        return builder
            .given("test state") // NOTE: Using provider states are optional, you can leave it out
            .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                .path("/")
                .method("GET")
                .headers(headers)
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
            .given("test state 2") // NOTE: Using provider states are optional, you can leave it out
            .uponReceiving("ExampleJavaConsumerPactTest second test interaction")
                .method("OPTIONS")
                .headers(headers)
                .path("/second")
                .body("")
            .willRespondWith()
                .status(200)
                .headers(headers)
                .body("")
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
    protected void runTest(String url) throws IOException {
        Assert.assertEquals(new ConsumerClient(url).options("/second"), 200);
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(url).getAsMap("/", ""), expectedResponse);
        assertEquals(new ConsumerClient(url).options("/second"), 200);
    }
}
```

### Using the Pact JUnit Rule

Thanks to [@warmuuh](https://github.com/warmuuh) we have a JUnit rule that simplifies running Pact consumer tests. To use it, create a test class
and then add the rule:

#### 1. Add the Pact Rule to your test class to represent your provider.

```java
    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("test_provider", "localhost", 8080, this);
```

The hostname and port are optional. If left out, it will default to localhost and a random available port.

#### 2. Annotate a method with Pact that returns a pact fragment for the provider and consumer

```java
    @Pact(provider="test_provider", consumer="test_consumer")
    public PactFragment createFragment(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("ExampleJavaConsumerPactRuleTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true}")
            .toFragment();
    }
```

##### Versions 3.0.2/2.2.13+

You can leave the provider name out. It will then use the provider name of the first mock provider found. I.e.,

```java
    @Pact(consumer="test_consumer") // will default to the provider name from mockProvider
    public PactFragment createFragment(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("ExampleJavaConsumerPactRuleTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true}")
            .toFragment();
    }
```

#### 3. Annotate your test method with PactVerification to have it run in the context of the mock server setup with the appropriate pact from step 1 and 2

```java
    @Test
    @PactVerification("test_provider")
    public void runTest() {
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        assertEquals(new ConsumerClient("http://localhost:8080").get("/"), expectedResponse);
    }
```

##### Versions 3.0.2/2.2.13+

You can leave the provider name out. It will then use the provider name of the first mock provider found. I.e.,

```java
    @Test
    @PactVerification
    public void runTest() {
        // This will run against mockProvider
        Map expectedResponse = new HashMap();
        expectedResponse.put("responsetest", true);
        assertEquals(new ConsumerClient("http://localhost:8080").get("/"), expectedResponse);
    }
```

For an example, have a look at [ExampleJavaConsumerPactRuleTest](src/test/java/au/com/dius/pact/consumer/examples/ExampleJavaConsumerPactRuleTest.java)

### Requiring a test with multiple providers

The Pact Rule can be used to test with multiple providers. Just add a rule to the test class for each provider, and
then include all the providers required in the `@PactVerification` annotation. For an example, look at
[PactMultiProviderTest](src/test/java/au/com/dius/pact/consumer/pactproviderrule/PactMultiProviderTest.java).

Note that if more than one provider fails verification for the same test, you will only receive a failure for one of them.
Also, to have multiple tests in the same test class, the providers must be setup with random ports (i.e. don't specify
a hostname and port). Also, if the provider name is left out of any of the annotations, the first one found will be used
(which may not be the first one defined).

### Requiring the mock server to run with HTTPS [versions 3.2.7/2.4.9+]

From versions 3.2.7/2.4.9+ the mock server can be started running with HTTPS using a self-signed certificate instead of HTTP.
To enable this set the `https` parameter to `true`.

E.g.:

```java
    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", "localhost", 8443, true,
      PactConfig.apply(PactSpecVersion.V2), this);                                                   // ^^^^
```

For an exmaple test doing this, see [PactProviderHttpsTest](src/test/java/au/com/dius/pact/consumer/pactproviderrule/PactProviderHttpsTest.java).

**NOTE:** The provider will start handling HTTPS requests using a self-signed certificate. Most HTTP clients will not accept
connections to a self-signed server as the certificate is untrusted. You may need to enable insecure HTTPS with your client
for this test to work. For an example of how to enable insecure HTTPS client connections with Apache Http Client, have a
look at [InsecureHttpsRequest](src/test/java/org/apache/http/client/fluent/InsecureHttpsRequest.java).

### Using the Pact DSL directly

Sometimes it is not convenient to use the ConsumerPactTest as it only allows one test per test class. The DSL can be
 used directly in this case.

Example:

```java
import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.ConsumerPactTest;
import au.com.dius.pact.consumer.PactError;
import au.com.dius.pact.consumer.TestRun;
import au.com.dius.pact.consumer.VerificationResult;
import au.com.dius.pact.consumer.examples.client.ProviderClient;
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

#### DSL Matching methods

The following matching methods are provided with the DSL. In most cases, they take an optional value parameter which
will be used to generate example values (i.e. when returning a mock response). If no example value is given, a random
one will be generated.

| method | description |
|--------|-------------|
| string, stringValue | Match a string value (using string equality) |
| number, numberValue | Match a number value (using Number.equals)\* |
| booleanValue | Match a boolean value (using equality) |
| stringType | Will match all Strings |
| numberType | Will match all numbers\* |
| integerType | Will match all numbers that are integers (both ints and longs)\* |
| decimalType | Will match all real numbers (floating point and decimal)\* |
| booleanType | Will match all boolean values (true and false) |
| stringMatcher | Will match strings using the provided regular expression |
| timestamp | Will match string containing timestamps. If a timestamp format is not given, will match an ISO timestamp format |
| date | Will match string containing dates. If a date format is not given, will match an ISO date format |
| time | Will match string containing times. If a time format is not given, will match an ISO time format |
| ipAddress | Will match string containing IP4 formatted address. |
| id | Will match all numbers by type |
| hexValue | Will match all hexadecimal encoded strings |
| uuid | Will match strings containing UUIDs |

_\* Note:_ JSON only supports double precision floating point values. Depending on the language implementation, they
may parsed as integer, floating point or decimal numbers.

#### Ensuring all items in a list match an example (2.2.0+)

Lots of the time you might not know the number of items that will be in a list, but you want to ensure that the list
has a minimum or maximum size and that each item in the list matches a given example. You can do this with the `arrayLike`,
`minArrayLike` and `maxArrayLike` functions.

| function | description |
|----------|-------------|
| `eachLike` | Ensure that each item in the list matches the provided example |
| `maxArrayLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `minArrayLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
    DslPart body = new PactDslJsonBody()
        .minArrayLike("users", 1)
            .id()
            .stringType("name")
            .closeObject()
        .closeArray();
```

This will ensure that the users list is never empty and that each user has an identifier that is a number and a name that is a string.

__Version 3.2.4/2.4.6+__ You can specify the number of example items to generate in the array. The default is 1.

```java
    DslPart body = new PactDslJsonBody()
        .minArrayLike("users", 1, 2)
            .id()
            .stringType("name")
            .closeObject()
        .closeArray();
```

This will generate the example body with 2 items in the users list.

#### Root level arrays that match all items (version 2.2.11+)

If the root of the body is an array, you can create PactDslJsonArray classes with the following methods:

| function | description |
|----------|-------------|
| `arrayEachLike` | Ensure that each item in the list matches the provided example |
| `arrayMinLike` | Ensure that each item in the list matches the provided example and the list is no bigger than the provided max |
| `arrayMaxLike` | Ensure that each item in the list matches the provided example and the list is no smaller than the provided min |

For example:

```java
PactDslJsonArray.arrayEachLike()
    .date("clearedDate", "mm/dd/yyyy", date)
    .stringType("status", "STATUS")
    .decimalType("amount", 100.0)
    .closeObject()
```

This will then match a body like:

```json
[ {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {
  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
}, {

  "clearedDate" : "07/22/2015",
  "status" : "C",
  "amount" : 15.0
} ]
```

__Version 3.2.4/2.4.6+__ You can specify the number of example items to generate in the array. The default is 1.

#### Matching JSON values at the root (Version 3.2.2/2.4.3+)

For cases where you are expecting basic JSON values (strings, numbers, booleans and null) at the root level of the body
and need to use matchers, you can use the `PactDslJsonRootValue` class. It has all the DSL matching methods for basic
values that you can use.

For example:

```java
.consumer("Some Consumer")
.hasPactWith("Some Provider")
    .uponReceiving("a request for a basic JSON value")
        .path("/hello")
    .willRespondWith()
        .status(200)
        .body(PactDslJsonRootValue.integerType())
```

#### Matching any key in a map (3.3.1/2.5.0+)

The DSL has been extended for cases where the keys in a map are IDs. For an example of this, see 
[#313](https://github.com/DiUS/pact-jvm/issues/131). In this case you can use the `eachKeyLike` method, which takes an 
example key as a parameter.

For example:

```java
DslPart body = new PactDslJsonBody()
  .object("one")
    .eachKeyLike("001", PactDslJsonRootValue.id(12345L)) // key like an id mapped to a matcher
  .closeObject()
  .object("two")
    .eachKeyLike("001-A") // key like an id where the value is matched by the following example
      .stringType("description", "Some Description")
    .closeObject()
  .closeObject()
  .object("three")
    .eachKeyMappedToAnArrayLike("001") // key like an id mapped to an array where each item is matched by the following example
      .id("someId", 23456L)
      .closeObject()
    .closeArray()
  .closeObject();

```

For an example, have a look at [WildcardKeysTest](src/test/java/au/com/dius/pact/consumer/WildcardKeysTest.java).

**NOTE:** The `eachKeyLike` method adds a `*` to the matching path, so the matching definition will be applied to all keys
 of the map if there is not a more specific matcher defined for a particular key. Having more than one `eachKeyLike` condition
 applied to a map will result in only one being applied when the pact is verified (probably the last).


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

### Matching on headers (version 2.2.2+)

You can use regular expressions to match request and response headers. The DSL has a `matchHeader` method for this. You can provide
an example header value to use when generating requests and responses, and if you leave it out it will generate a random one
from the regular expression.

For example:

```java
  .given("test state")
    .uponReceiving("a test interaction")
        .path("/hello")
        .method("POST")
        .matchHeader("testreqheader", "test.*value")
        .body("{\"name\": \"harry\"}")
    .willRespondWith()
        .status(200)
        .body("{\"hello\": \"harry\"}")
        .matchHeader("Location", ".*/hello/[0-9]+", "/hello/1234")
```

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

# Publishing your pact files to a pact broker

If you use Gradle, you can use the [pact Gradle plugin](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-provider-gradle#publishing-pact-files-to-a-pact-broker) to publish your pact files.

# Pact Specification V3

Version 3 of the pact specification changes the format of pact files in the following ways:

* Query parameters are stored in a map form and are un-encoded (see [#66](https://github.com/DiUS/pact-jvm/issues/66)
and [#97](https://github.com/DiUS/pact-jvm/issues/97) for information on what this can cause).
* Introduces a new message pact format for testing interactions via a message queue.
* Multiple provider states can be defined with data parameters.

## Generating V3 spec pact files (3.1.0+, 2.3.0+)

To have your consumer tests generate V3 format pacts, you can set the specification version to V3. If you're using the
`ConsumerPactTest` base class, you can override the `getSpecificationVersion` method. For example:

```java
    @Override
    protected PactSpecVersion getSpecificationVersion() {
        return PactSpecVersion.V3;
    }
```

If you are using the `PactProviderRule`, you can pass the version into the constructor for the rule.

```java
    @Rule
    public PactProviderRule mockTestProvider = new PactProviderRule("test_provider", PactSpecVersion.V3, this);
```

## Consumer test for a message consumer

For testing a consumer of messages from a message queue, the `MessagePactProviderRule` rule class works in much the
same way as the `PactProviderRule` class for Request-Response interactions, but will generate a V3 format message pact file.

For an example, look at [ExampleMessageConsumerTest](https://github.com/DiUS/pact-jvm/blob/master/pact-jvm-consumer-junit%2Fsrc%2Ftest%2Fjava%2Fau%2Fcom%2Fdius%2Fpact%2Fconsumer%2Fv3%2FExampleMessageConsumerTest.java)

