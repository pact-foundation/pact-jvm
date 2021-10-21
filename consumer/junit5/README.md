pact-jvm-consumer-junit5
========================

JUnit 5 support for Pact consumer tests

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius.pact.consumer`
* artifact-id = `junit5`
* version-id = `4.2.X`

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
    @Pact(provider="ArticlesProvider", consumer="test_consumer")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("test state")
            .uponReceiving("ExampleJavaConsumerPactTest test interaction")
                .path("/articles.json")
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
[ArticlesTest](https://github.com/DiUS/pact-jvm/blob/master/consumer/junit5/src/test/java/au/com/dius/pact/consumer/junit5/ArticlesTest.java) and
[MultiTest](https://github.com/DiUS/pact-jvm/blob/master/consumer/junit5/src/test/groovy/au/com/dius/pact/consumer/junit5/MultiTest.groovy).

The `@PactTestFor` annotation allows you to control the mock server in the same way as the JUnit 4 `PactProviderRule`. It
allows you to set the hostname to bind to (default is `localhost`) and the port (default is to use a random port). You
can also set the Pact specification version to use (default is V3).

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ArticlesProvider")
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
same provider name will be used. See [ArticlesTest](https://github.com/DiUS/pact-jvm/blob/master/consumer/junit5/src/test/java/au/com/dius/pact/consumer/junit5/ArticlesTest.java) for
an example.

#### Matching the interactions by method name

If you set the `pactMethod` on the `@PactTestFor` annotation, then the method with the provided name will be used (it still
needs a `@Pact` annotation). See [MultiTest](https://github.com/DiUS/pact-jvm/blob/master/consumer/junit5/src/test/groovy/au/com/dius/pact/consumer/junit5/MultiTest.groovy) for an example.

### Injecting the mock server into the test

You can get the mock server injected into the test method by adding a `MockServer` parameter to the test method.

```java
  @Test
  void test(MockServer mockServer) throws IOException {
    HttpResponse httpResponse = Request.Get(mockServer.getUrl() + "/articles.json").execute().returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
  }
```

This helps with getting the base URL of the mock server, especially when a random port is used.

## Changing the directory pact files are written to

By default, pact files are written to `target/pacts` (or `build/pacts` if you use Gradle), but this can be overwritten with the `pact.rootDir` system property.
This property needs to be set on the test JVM as most build tools will fork a new JVM to run the tests.

For Gradle, add this to your build.gradle:

```groovy
test {
    systemProperties['pact.rootDir'] = "$buildDir/custom-pacts-directory"
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

### Using `@PactDirectory` annotation

You can override the directory the pacts are written in a test by adding the `@PactDirectory` annotation to the test
class.

## Forcing pact files to be overwritten

By default, when the pact file is written, it will be merged with any existing pact file. To force the file to be 
overwritten, set the Java system property `pact.writer.overwrite` to `true`.

# Having values injected from provider state callbacks

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

The following DSL methods all you to set an expression that will be parsed with the values returned from the provider states:

For JSON bodies, use `valueFromProviderState`.<br/>
For headers, use `headerFromProviderState`.<br/>
For query parameters, use `queryParameterFromProviderState`.<br/>
For paths, use `pathFromProviderState`.

For example, assume that an API call is made to get the details of a user by ID. A provider state can be defined that
specifies that the user must be exist, but the ID will be created when the user is created. So we can then define an
expression for the path where the ID will be replaced with the value returned from the provider state callback.

```java
    .pathFromProviderState("/api/users/${id}", "/api/users/100")
``` 
You can also just use the key instead of an expression:

```java
    .valueFromProviderState('userId', 'userId', 100) // will look value using userId as the key
```

## Overriding the expression markers `${` and `}` (4.1.25+)

You can change the markers of the expressions using the following system properties:
- `pact.expressions.start` (default is `${`)
- `pact.expressions.end` (default is `}`)

## Using HTTPS

You can enable a HTTPS mock server by setting `https=true` on the `@PactTestFor` annotation. Note that this mock
server will use a self-signed certificate, so any client code will need to accept self-signed certificates.

## Using own KeyStore

You can provide your own KeyStore file to be loaded on the MockServer. In order to do so you should fulfill the 
properties `keyStorePath`, `keyStoreAlias`, `keyStorePassword`, `privateKeyPassword` on the `@PactTestFor` annotation.
Please bear in mind you should also enable HTTPS flag.

## Using multiple providers in a test (4.2.5+)

It is advisable to focus on a single interaction with each test, but you can enable multiple providers in a single test.
In this case, a separate mock server will be started for each configured provider.

To enable this:

1. Create a method to create the Pact for each provider annotated with the `@Pact(provider = "....")` annotation. The
    provider name must be set on the annotation. You can create as many of these as required, but each must have a unique 
    provider name.
2. In the test method, use the `pactMethods` attribute on the `@PactTestFor` annotation with the names of all the 
    methods defined in step 1.
3. Add a MockServer parameter to the test method for each provider configured in step 1 with a `@ForProvider` 
    annotation with the name of the provider.
4. In your test method, interact with each of the mock servers passed in step 3. Note that if any mock server does not
    get the requests it expects, it will fail the test.
   
For an example, see [MultiProviderTest](https://github.com/DiUS/pact-jvm/blob/master/consumer/junit5/src/test/groovy/au/com/dius/pact/consumer/junit5/MultiProviderTest.groovy).

## Dealing with persistent HTTP/1.1 connections (Keep Alive)

As each test will get a new mock server, connections can not be persisted between tests. HTTP clients can cache
connections with HTTP/1.1, and this can cause subsequent tests to fail. See [#342](https://github.com/pact-foundation/pact-jvm/issues/342)
and [#1383](https://github.com/pact-foundation/pact-jvm/issues/1383).

One option (if the HTTP client supports it, Apache HTTP Client does) is to set the system property `http.keepAlive` to `false` in
the test JVM. The other option is to set `pact.mockserver.addCloseHeader` to `true` to force the mock server to
send a `Connection: close` header with every response (supported with Pact-JVM 4.2.7+).

# Testing messages

You can use Pact to test interactions with messaging systems. There are two main types of message support: asynchronous 
messages and synchronous request/response messages.

## Asynchronous messages

Asynchronous messages are you normal type of single shot or fire and forget type messages. They are typically sent to a
message queue or topic as a notification or event. With Pact tests, we will be testing that our consumer of the messages
works with the messages setup as the expectations in test. This should be the message handler code that processes the
actual messages that come off the message queue in production.

You can use either the V3 Message Pact or the V4 Asynchronous Message interaction to test these types of interactions.

For a V3 message pact example, see [AsyncMessageTest](https://github.com/pact-foundation/pact-jvm/blob/ac6a0eae0b18183f6f453eafddb89b90741ace42/consumer/junit5/src/test/java/au/com/dius/pact/consumer/junit5/AsyncMessageTest.java).

For a V4 asynchronous message example, see [V4AsyncMessageTest](https://github.com/pact-foundation/pact-jvm/blob/master/consumer/junit5/src/test/groovy/au/com/dius/pact/consumer/junit5/V4AsyncMessageTest.groovy).

### Matching message metadata

You can also use matching rules for the metadata associated with the message. There is a `MetadataBuilder` class to
help with this. You can access it via the `withMetadata` method that takes a Java Consumer on the `MessagePactBuilder` class.

For example:

```java
builder.given("SomeProviderState")
    .expectsToReceive("a test message with metadata")
    .withMetadata(md -> {
        md.add("metadata1", "metadataValue1");
        md.add("metadata2", "metadataValue2");
        md.add("metadata3", 10L);
        md.matchRegex("partitionKey", "[A-Z]{3}\\d{2}", "ABC01");
    })
    .withContent(body)
    .toPact();
```

### V4 Synchronous request/response messages

Synchronous request/response messages are a form of message interchange were a request message is sent to another service and
one or more response messages are returned. Examples of this would be things like Websockets and gRPC.

For a V4 synchronous request/response message example, see [V4AsyncMessageTest](https://github.com/pact-foundation/pact-jvm/blob/master/consumer/junit5/src/test/groovy/au/com/dius/pact/consumer/junit5/V4SyncMessageTest.groovy).

# Using Pact plugins (version 4.3.0+)

The `PactBuilder` consumer test builder supports using Pact plugins. Plugins are defined in the [Pact plugins project](https://github.com/pact-foundation/pact-plugins).
To use plugins requires the use of Pact specification V4 Pacts.

To use a plugin, first you need to let the builder know to load the plugin (using the `usingPlugin` method) and then 
configure the interaction based on the requirements for the plugin. Each plugin may have different requirements, so you 
will have to consult the plugin docs on what is required. The plugins will be loaded from the plugin directory. By 
default, this is `~/.pact/plugins` or the value of the `PACT_PLUGIN_DIR` environment variable.

Then you need to use the `with` method that takes a Map-based data structure and passed it on to the plugin to
setup the interaction.

For example, if we use the CSV plugin from the plugins project, our test would look like:

```java
@ExtendWith(PactConsumerTestExt.class)
class CsvClientTest {
  /**
   * Setup an interaction that makes a request for a CSV report 
   */
  @Pact(consumer = "CsvClient")
  V4Pact pact(PactBuilder builder) {
    return builder
      // Tell the builder to load the CSV plugin
      .usingPlugin("csv")
      // Interaction we are expecting to receive
      .expectsToReceive("request for a report", "core/interaction/http")
      // Data for the interaction. This will be sent to the plugin
      .with(Map.of(
        "request.path", "/reports/report001.csv",
        "response.status", "200",
        "response.contents", Map.of(
          "pact:content-type", "text/csv",
          "csvHeaders", false,
          "column:1", "matching(type,'Name')",
          "column:2", "matching(number,100)",
          "column:3", "matching(datetime, 'yyyy-MM-dd','2000-01-01')"
        )
      ))
      .toPact();
  }

  /**
   * Test to get the CSV report
   */
  @Test
  @PactTestFor(providerName = "CsvServer", pactMethod = "pact")
  void getCsvReport(MockServer mockServer) throws IOException {
    // Setup our CSV client class to point to the Pact mock server
    CsvClient client = new CsvClient(mockServer.getUrl());
    
    // Fetch the CSV report
    List<CSVRecord> csvData = client.fetch("report001.csv", false);
    
    // Verify it is as expected
    assertThat(csvData.size(), is(1));
    assertThat(csvData.get(0).get(0), is(equalTo("Name")));
    assertThat(csvData.get(0).get(1), is(equalTo("100")));
    assertThat(csvData.get(0).get(2), matchesRegex("\\d{4}-\\d{2}-\\d{2}"));
  }
}
```
