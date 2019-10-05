# Pact junit runner

## Overview
Library provides ability to play contract tests against a provider service in JUnit fashionable way.

Supports:

- Out-of-the-box convenient ways to load pacts

- Easy way to change assertion strategy

- **org.junit.BeforeClass**, **org.junit.AfterClass** and **org.junit.ClassRule** JUnit annotations, that will be run
once - before/after whole contract test suite.

- **org.junit.Before**, **org.junit.After** and **org.junit.Rule** JUnit annotations, that will be run before/after
each test of an interaction.

- **au.com.dius.pact.provider.junit.State** custom annotation - before each interaction that requires a state change,
all methods annotated by `@State` with appropriate the state listed will be invoked. These methods must either take
no parameters or a single Map parameter.

## Example of HTTP test

```java
    @RunWith(PactRunner.class) // Say JUnit to run tests with custom Runner
    @Provider("myAwesomeService") // Set up name of tested provider
    @PactFolder("pacts") // Point where to find pacts (See also section Pacts source in documentation)
    public class ContractTest {
        // NOTE: this is just an example of embedded service that listens to requests, you should start here real service
        @ClassRule //Rule will be applied once: before/after whole contract test suite
        public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);

        @BeforeClass //Method will be run once: before whole contract test suite
        public static void setUpService() {
            //Run DB, create schema
            //Run service
            //...
        }

        @Before //Method will be run before each test of interaction
        public void before() {
            // Rest data
            // Mock dependent service responses
            // ...
            embeddedService.addExpectation(
                    onRequestTo("/data"), giveEmptyResponse()
            );
        }

        @State("default", "no-data") // Method will be run before testing interactions that require "default" or "no-data" state
        public void toDefaultState() {
            // Prepare service before interaction that require "default" state
            // ...
            System.out.println("Now service in default state");
        }
        
        @State("with-data") // Method will be run before testing interactions that require "with-data" state
        public void toStateWithData(Map data) {
            // Prepare service before interaction that require "with-data" state. The provider state data will be passed 
            // in the data parameter
            // ...
            System.out.println("Now service in state using data " + data);
        }

        @TestTarget // Annotation denotes Target that will be used for tests
        public final Target target = new HttpTarget(8332); // Out-of-the-box implementation of Target (for more information take a look at Test Target section)
    }
```

## Example of AMQP Message test

```java
    @RunWith(PactRunner.class) // Say JUnit to run tests with custom Runner
    @Provider("myAwesomeService") // Set up name of tested provider
    @PactBroker(host="pactbroker", port = "80") 
    public class ConfirmationKafkaContractTest {

        @TestTarget // Annotation denotes Target that will be used for tests
        public final Target target = new AmqpTarget(); // Out-of-the-box implementation of Target (for more information take a look at Test Target section)

        @BeforeClass //Method will be run once: before whole contract test suite
        public static void setUpService() {
            //Run DB, create schema
            //Run service
            //...
        }

        @Before //Method will be run before each test of interaction
        public void before() {
            // Message data preparation
            // ...
        }

        @PactVerifyProvider('an order confirmation message')
        String verifyMessageForOrder() {
            Order order = new Order()
            order.setId(10000004)
            order.setPrice(BigDecimal.TEN)
            order.setUnits(15)

            def message = new ConfirmationKafkaMessageBuilder()
              .withOrder(order)
              .build()

            JsonOutput.toJson(message)
        }

    }
```

## Provider state callback methods

For the provider states in the pact being verified, you can define methods to be invoked to setup the correct state
for each interaction. Just annotate a method with the `au.com.dius.pact.provider.junit.State` annotation and the
method will be invoked before the interaction is verified.

For example:

```java
@State("SomeProviderState") // Must match the state description in the pact file
public void someProviderState() {
  // Do what you need to set the correct state
}
```

If there are parameters in the pact file, just add a Map parameter to the method to be able to access those parameters.

```java
@State("SomeProviderState")
public void someProviderState(Map<String, Object> providerStateParameters) {
  // Do what you need to set the correct state
}
```

### Provider state teardown methods [3.5.22+]

If you need to tear down your provider state, you can annotate a method with the `@State` annotation with the action
set to `StateChangeAction.TEARDOWN` and it will be invoked after the interaction is verified.

```java
@State("SomeProviderState", action = StateChangeAction.TEARDOWN)
public void someProviderStateCleanup() {
  // Do what you need to to teardown the state
}
```

#### Returning values that can be injected (3.6.11+)

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

There are methods on the consumer DSLs that can provider an expression that contains variables (like '/api/user/${id}'
for the path). The provider state callback can then return a map for values, and the `id` attribute from the map will
be expanded in the expression. For this to work, just make your provider state method return a Map of the values.

## Pact source

The Pact runner will automatically collect pacts based on annotations on the test class. For this purpose there are 3
out-of-the-box options (files from a directory, files from a set of URLs or a pact broker) or you can easily add your
own Pact source.

If you need to load a single pact file from the file system, use the `PactUrl` with the URL set to the file path.

**Note:** You can only define one source of pacts per test class.

### Download pacts from a pact-broker

To use pacts from a Pact Broker, annotate the test class with `@PactBroker(host="host.of.pact.broker.com", port = "80")`.

You can also specify the protocol, which defaults to "http".

The pact broker will be queried for all pacts with the same name as the provider annotation.

For example, test all pacts for the "Activity Service" in the pact broker:

```java
@RunWith(PactRunner.class)
@Provider("Activity Service")
@PactBroker(host = "localhost", port = "80")
public class PactJUnitTest {

  @TestTarget
  public final Target target = new HttpTarget(5050);

}
```

#### Using Java System properties

The pact broker loader was updated to allow system properties to be used for the hostname, port or protocol. The port
was changed to a string to allow expressions to be set.

To use a system property or environment variable, you can place the property name in `${}` expression de-markers:

```java
@PactBroker(host="${pactbroker.hostname}", port = "80")
```

You can provide a default value by separating the property name with a colon (`:`):

```java
@PactBroker(host="${pactbroker.hostname:localhost}", port = "80")
```

#### More Java System properties

The default values of the `@PactBroker` annotation now enable variable interpolation.
The following keys may be managed through the environment
* `pactbroker.host`
* `pactbroker.port`
* `pactbroker.scheme`
* `pactbroker.tags` (comma separated)
* `pactbroker.auth.scheme` (basic or bearer)
* `pactbroker.auth.username` (for basic auth)
* `pactbroker.auth.password` (for basic auth)
* `pactbroker.auth.token` (for bearer auth)


#### Using tags with the pact broker

The pact broker allows different versions to be tagged. To load all the pacts:

```java
@PactBroker(host="pactbroker", port = "80", tags = {"latest", "dev", "prod"})
```

The default value for tags is `latest` which is not actually a tag but instead corresponds to the latest version ignoring the tags. If there are multiple consumers matching the name specified in the provider annotation then the latest pact for each of the consumers is loaded.

For any other value the latest pact tagged with the specified tag is loaded.

Specifying multiple tags is an OR operation. For example if you specify `tags = {"dev", "prod"}` then both the latest pact file tagged with `dev` and the latest pact file taggged with `prod` is loaded.

#### Using authentication with the with the pact broker

You can use basic authentication with the `@PactBroker` annotation by setting the `authentication` value to a `@PactBrokerAuth`
annotation. For example:

```java
@PactBroker(host = "${pactbroker.url:localhost}", port = "1234", tags = {"latest", "prod", "dev"},
  authentication = @PactBrokerAuth(username = "test", password = "test"))
```

Bearer tokens are also supported. For example:

```java
@PactBroker(host = "${pactbroker.url:localhost}", port = "1234", tags = {"latest", "prod", "dev"},
  authentication = @PactBrokerAuth(scheme = "bearer", token = "test"))
```

The `scheme`, `token`, `username` and `password` values also take Java system property expressions.

Preemptive Authentication can be enabled by setting the `pact.pactbroker.httpclient.usePreemptiveAuthentication` Java
system property to `true`.

### Pact Url

To use pacts from urls annotate the test class with

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"} )
```

If you need to load a single pact file from the file system, you can use the `PactUrl` with the URL set to the file path.

### Pact folder

To use pacts from a resource folder of the project annotate test class with

```java
@PactFolder("subfolder/in/resource/directory")
```

### Custom pacts source

It's possible to use a custom Pact source. For this, implement interface `au.com.dius.pact.provider.junit.loader.PactLoader`
and annotate the test class with `@PactSource(MyOwnPactLoader.class)`. **Note:** class `MyOwnPactLoader` must have a default empty constructor or a constructor with one argument of class `Class` which at runtime will be the test class so you can get custom annotations of test class.

### Filtering the interactions that are verified

By default, the pact runner will verify all pacts for the given provider. You can filter the pacts and interactions by
the following methods.

#### Filtering by Consumer

You can run only those pacts for a particular consumer by adding a `@Consumer` annotation to the test class.

For example:

```java
@RunWith(PactRunner.class)
@Provider("Activity Service")
@Consumer("Activity Consumer")
@PactBroker(host = "localhost", port = "80")
public class PactJUnitTest {

  @TestTarget
  public final Target target = new HttpTarget(5050);

}
```

#### Filtering by Provider State

You can filter the interactions that are executed by adding a `@PactFilter` annotation to your test class. The pact 
filter annotation will then only verify interactions that have a matching provider state. You can provide multiple 
states to match with.

For example: 

```java
@RunWith(PactRunner.class)
@Provider("Activity Service")
@PactBroker(host = "localhost", port = "80")
@PactFilter('Activity 100 exists in the database')
public class PactJUnitTest {

  @TestTarget
  public final Target target = new HttpTarget(5050);

}
```

You can also use regular expressions with the filter. For example:

```java
@RunWith(PactRunner.class)
@PactFilter('Activity \\d+ exists in the database')
public class PactJUnitTest {

}
```

### Setting the test to not fail when no pacts are found

By default the pact runner will fail the verification test if no pact files are found to verify. To change the
failure into a warning, add a `@IgnoreNoPactsToVerify` annotation to your test class.

#### Ignoring IO errors loading pact files

You can also set the test to ignore any IO and parser exceptions when loading the pact files by setting the
`ignoreIoErrors` attribute on the annotation to `"true"` or setting the JVM system property `pact.verification.ignoreIoErrors`
to `true`.

** WARNING! Do not enable this on your CI server, as this could result in your build passing with no providers 
having been verified due to a configuration error. **        

## Test target

The field in test class of type `au.com.dius.pact.provider.junit.target.Target` annotated with `au.com.dius.pact.provider.junit.target.TestTarget`
will be used for actual Interaction execution and asserting of contract.

**Note:** there must be exactly 1 such field, otherwise an `InitializationException` will be thrown.

### HttpTarget

`au.com.dius.pact.provider.junit.target.HttpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as http request and assert response from service by matching rules from pact.

You can also specify the protocol, defaults to "http".

### AmqpTarget

`au.com.dius.pact.provider.junit.target.AmqpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as an AMQP message and assert response from service by matching rules from pact.

**Note for Maven users:** If you use Maven to run your tests, you will have to make sure that the Maven Surefire plugin is at least
  version 2.22.1 uses an isolated classpath.

For example, configure it by adding the following to your POM: 

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>2.22.1</version>
    <configuration>
        <useSystemClassLoader>false</useSystemClassLoader>
    </configuration>
</plugin>
```

#### Modifying the requests before they are sent

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The HttpTarget supports request filters by annotating methods
on the test class with `@TargetRequestFilter`. These methods must be public void methods that take a single HttpRequest
parameter.

For example:

```java
    @TargetRequestFilter
    public void exampleRequestFilter(HttpRequest request) {
      request.addHeader("Authorization", "OAUTH hdsagasjhgdjashgdah...");
    }
```

__*Important Note:*__ You should only use this feature for things that can not be persisted in the pact file. By modifying
the request, you are potentially modifying the contract from the consumer tests!

#### Turning off URL decoding of the paths in the pact file

By default the paths loaded from the pact file will be decoded before the request is sent to the provider. To turn this
behaviour off, set the system property `pact.verifier.disableUrlPathDecoding` to `true`.

__*Important Note:*__ If you turn off the url path decoding, you need to ensure that the paths in the pact files are 
correctly encoded. The verifier will not be able to make a request with an invalid encoded path.

### Custom Test Target

It's possible to use custom `Target`, for that interface `Target` should be implemented and this class can be used instead of `HttpTarget`.

# Verification Reports

The default test behaviour is to display the verification being done to the console, and pass or fail the test via the normal
JUnit mechanism. Additional reports can be generated from the tests.

## Enabling additional reports via annotations on the test classes

A `@VerificationReports` annotation can be added to any pact test class which will control the verification output. The
annotation takes a list report types and an optional report directory (defaults to "target/pact/reports").
The currently supported report types are `console`, `markdown` and `json`.

For example:

```java
@VerificationReports({"console", "markdown"})
public class MyPactTest {
```

will enable the markdown report in addition to the normal console output. And,

```java
@VerificationReports(value = {"markdown"}, reportDir = "/myreports")
public class MyPactTest {
```

will disable the normal console output and write the markdown reports to "/myreports".

## Enabling additional reports via Java system properties or environment variables

The additional reports can also be enabled with Java System properties or environment variables. The following two
properties have been introduced: `pact.verification.reports` and `pact.verification.reportDir`.

`pact.verification.reports` is the comma separated list of report types to enable (e.g. `console,json,markdown`).
`pact.verification.reportDir` is the directory to write reports to (defaults to "target/pact/reports").

## Additional Reports

The following report types are available in addition to console output (`console`, which is enabled by default):
`markdown`, `json`.

You can also provide a fully qualified classname as report so custom reports are also supported.
This class must implement `au.com.dius.pact.provider.reporters.VerifierReporter` interface in order to be correct custom implementation of a report.

# Publishing verification results to a Pact Broker

For pacts that are loaded from a Pact Broker, the results of running the verification can be published back to the
 broker against the URL for the pact. You will be able to see the result on the Pact Broker home screen. You need to
 set the version of the provider that is verified using the `pact.provider.version` system property.
 
To enable publishing of results, set the property `pact.verifier.publishResults` to `true`.
