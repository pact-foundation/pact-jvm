pact-jvm-consumer-junit5
========================

JUnit 5 support for Pact consumer tests

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius`
* artifact-id = `pact-jvm-consumer-junit5_2.12`
* version-id = `3.5.x`

## Usage

### 1. Add the Pact consumer test extension to the test class.

To write Pact consumer tests with JUnit 5, you need to add `@ExtendWith(PactConsumerTestExt)` to your test class. This
replaces the `PactRunner` used for JUnit 4 tests. The rest of the test follows a similar pattern as for JUnit 4 tests.

```java
@ExtendWith(PactConsumerTestExt.class)
class ExampleJavaConsumerPactTest {
```

### 2. create a method annotated with `@Pact` that returns the interactions for the test

For each test (as with JUnit 4), you need to define a method annotated with the `@Pact` annotation that returns the
interactions for the test.

```java
    @Pact(provider="test_provider", consumer="test_consumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                .path("/")
                .method("GET")
            .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true}")
            .toPact();
    }
```

### 3. Link the mock server with the interactions for the test with `@PactTestFor`

Then the final step is to use the `@PactTestFor` annotation to tell the Pact extension how to setup the Pact test. You
can either put this annotation on the test class, or on the test method. For examples see
[ArticlesTest](src/test/java/au/com/dius/pact/consumer/junit5/ArticlesTest.java) and
[MultiTest](src/test/groovy/au/com/dius/pact/consumer/junit5/MultiTest.groovy).

The `@PactTestFor` annotation allows you to control the mock server in the same way as the JUnit 4 `PactProviderRule`. It
allows you to set the hostname to bind to (default is `localhost`) and the port (default is to use a random port). You
can also set the Pact specification version to use (default is V3).

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ArticlesProvider", port = "1234")
public class ExampleJavaConsumerPactTest {
```

**NOTE on the hostname**: The mock server runs in the same JVM as the test, so the only valid values for hostname are:

| hostname | result |
| -------- | ------ |
| `localhost` | binds to the address that localhost points to (normally the loopback adapter) |
| `127.0.0.1` or `::1` | binds to the loopback adapter |
| host name | binds to the default interface that the host machines DNS name resolves to |
| `0.0.0.0` or `::` | binds to the all interfaces on the host machine |

#### Matching the interactions by provider name

If you set the `providerName` on the `@PactTestFor` annotation, then the first method with a `@Pact` annotation with the
same provider name will be used. See [ArticlesTest](src/test/java/au/com/dius/pact/consumer/junit5/ArticlesTest.java) for
an example.

#### Matching the interactions by method name

If you set the `pactMethod` on the `@PactTestFor` annotation, then the method with the provided name will be used (it still
needs a `@Pact` annotation). See [MultiTest](src/test/groovy/au/com/dius/pact/consumer/junit5/MultiTest.groovy) for an example.

### Injecting the mock server into the test

You can get the mock server injected into the test method by adding a `MockServer` parameter to the test method.

```java
  @Test
  void test(MockServer mockServer) {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
  }
```

This helps with getting the base URL of the mock server, especially when a random port is used.

## Unsupported

The current implementation does not support tests with multiple providers. This will be added in a later release.
