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

## Example of test

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

## Pact source

The Pact runner will automatically collect pacts based on annotations on the test class. For this purpose there are 3
out-of-the-box options (files from a directory, files from a set of URLs or a pact broker) or you can easily add your
own Pact source.

**Note:** You can only define one source of pacts per test class.

### Download pacts from a pact-broker

To use pacts from a Pact Broker, annotate the test class with `@PactBroker(host="host.of.pact.broker.com", port = "80")`.

From _version 3.2.2/2.4.3+_ you can also specify the protocol, which defaults to "http".

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

#### _Version 3.2.3/2.4.4+_ - Using Java System properties

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

#### _Version 3.2.4/2.4.6+_ - Using tags with the pact broker

The pact broker allows different versions to be tagged. To load all the pacts:

```java
@PactBroker(host="pactbroker", port = "80", tags = {"latest", "dev", "prod"})
```

The `latest` tag corresponds to the latest version ignoring the tags, and is the default.

### Pact Url

To use pacts from urls annotate the test class with

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"} )
```

### Pact folder

To use pacts from a resource folder of the project annotate test class with

```java
@PactFolder("subfolder/in/resource/directory")
```

### Custom pacts source

It's possible to use a custom Pact source. For this, implement interface `au.com.dius.pact.provider.junit.loader.PactLoader`
and annotate the test class with `@PactSource(MyOwnPactLoader.class)`. **Note:** class `MyOwnPactLoader` must have a default empty constructor or a constructor with one argument of class `Class` which at runtime will be the test class so you can get custom annotations of test class.

## Test target

The field in test class of type `au.com.dius.pact.provider.junit.target.Target` annotated with `au.com.dius.pact.provider.junit.target.TestTarget`
will be used for actual Interaction execution and asserting of contract.

**Note:** there must be exactly 1 such field, otherwise an `InitializationException` will be thrown.

### HttpTarget

`au.com.dius.pact.provider.junit.target.HttpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as http request and assert response from service by matching rules from pact.

_Version 3.2.2/2.4.3+_ you can also specify the protocol, defaults to "http".

#### Modifying the requests before they are sent [Version 3.2.3/2.4.5+]

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

### Custom Test Target

It's possible to use custom `Target`, for that interface `Target` should be implemented and this class can be used instead of `HttpTarget`.

# Verification Reports [versions 3.2.7/2.4.9+]

The default test behaviour is to display the verification being done to the console, and pass or fail the test via the normal
JUnit mechanism. From versions 3.2.7/2.4.9+, additional reports can be generated from the tests.

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
