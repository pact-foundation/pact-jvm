# Pact junit runner

## Overview
Library provides ability to play contract tests against provider service in JUnit fashionable way.

Supports:

- Out-of-the-box convenient ways to load pacts

- Easy way to change assertion strategy

- **org.junit.BeforeClass**, **org.junit.AfterClass** and **org.junit.ClassRule** JUnit annotations, that will be run once - before/after whole contract test suite

- **org.junit.Before**, **org.junit.After** and **org.junit.Rule** JUnit annotations, that will be run before/after each test of interaction

- **au.com.dius.pact.provider.junit.State** custom annotation - before each interaction that require state change, all methods annotated by State with appropriate state listed will be invoked

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

## Pacts source

Pact runner will automatically collect pacts: for this purpose there are 2 out-of-the-box options or you can easily add your own Pact source. **Note:** it's possible to use only one source of pacts.

### Download pacts from pact-broker

To use pacts from Pact Broker annotate test class with `@PactBroker(host="host.of.pact.broker.com", port = 80)`.

### Pact Url

To use pacts from urls annotate test class with `@PactUrl(urls = {http://build.server/zoo_app-animal_service.json} )`.

### Pact folder

To use pacts from resource folder of project annotate test class with `@PactFolder("subfolder/in/resource/directory")`.

### Custom pacts source

It's possible to use custom Pact source: for this implement `interface au.com.dius.pact.provider.junit.loader.PactLoader` and annotate test class with `@PactSource(MyOwnPactLoader.class)`. **Note:** `class MyOwnPactLoader` should have default constructor.

## Test target

Field in test class of type `au.com.dius.pact.provider.junit.target.Target` annotated with `au.com.dius.pact.provider.junit.target.TestTarget` will be used for actual Interaction execution and asserting of contract. **Note:** should be exactly 1 such field, otherwise `InitializationException` will be thrown.

### HttpTarget

`au.com.dius.pact.provider.junit.target.HttpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target` that will play pacts as http request and assert response from service by matching rules from pact.

### Custom Test Target

It's possible to use custom `Target` for that `interface Target` should be implemented and this class can be used instead of `HttpTarget`.