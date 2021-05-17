pact-jvm-consumer-junit5
========================

JUnit 5 support for Pact consumer tests

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius.pact.consumer`
* artifact-id = `junit5`
* version-id = `4.1.0`

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

## Unsupported

The current implementation does not support tests with multiple providers. This will be added in a later release.

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

## Using HTTPS

You can enable a HTTPS mock server by setting `https=true` on the `@PactTestFor` annotation. Note that this mock
server will use a self-signed certificate, so any client code will need to accept self-signed certificates.

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

# Message Pacts
## Consumer test for a message consumer
For testing a consumer of messages from a message queue using JUnit 5 and Pact V4, see [AsyncMessageTest](https://github.com/pact-foundation/pact-jvm/blob/ac6a0eae0b18183f6f453eafddb89b90741ace42/consumer/junit5/src/test/java/au/com/dius/pact/consumer/junit5/AsyncMessageTest.java).
