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
all methods annotated by `@State` with appropriate the state listed will be invoked.

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

### Pact Url

To use pacts from urls annotate the test class with

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"} )
```

### Pact folder

To use pacts from a resource folder of the project (or any folder) annotate test class with

```java
@PactFolder("subfolder/in/resource/directory")
```

### Custom pacts source

It's possible to use a custom Pact source. For this, implement interface `au.com.dius.pact.provider.junit.loader.PactLoader`
and annotate the test class with `@PactSource(MyOwnPactLoader.class)`. **Note:** class `MyOwnPactLoader` must have a default constructor.

## Test target

The field in test class of type `au.com.dius.pact.provider.junit.target.Target` annotated with `au.com.dius.pact.provider.junit.target.TestTarget`
will be used for actual Interaction execution and asserting of contract.

**Note:** there must be exactly 1 such field, otherwise an `InitializationException` will be thrown.

### HttpTarget

`au.com.dius.pact.provider.junit.target.HttpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as http request and assert response from service by matching rules from pact.

_Version 3.2.2/2.4.3+_ you can also specify the protocol, defaults to "http".

### Custom Test Target

It's possible to use custom `Target`, for that interface `Target` should be implemented and this class can be used instead of `HttpTarget`.
