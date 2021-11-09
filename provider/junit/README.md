# Pact junit runner

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius.pact.provider`
* artifact-id = `junit`
* version-id = `4.1.x`


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

        @State({"default", "no-data"}) // Method will be run before testing interactions that require "default" or "no-data" state
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

## Example of Message test

```java
    @RunWith(PactRunner.class) // Say JUnit to run tests with custom Runner
    @Provider("myAwesomeService") // Set up name of tested provider
    @PactBroker(host="pactbroker", port = "80") 
    public class ConfirmationKafkaContractTest {

        @TestTarget // Annotation denotes Target that will be used for tests
        public final Target target = new MessageTarget(); // Out-of-the-box implementation of Target (for more information take a look at Test Target section)

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

### Example of Message test that verifies metadata

To have the message metadata - such as the topic - also verified you need to return a `MessageAndMetadata` from 
the invoked method that contains the payload and metadata to be validation. For example, to verify the metadata of an 
integration using the Spring [Message](https://docs.spring.io/spring-integration/reference/html/message.html) interface, 
you can do something like the following:

```java
  ...

  @PactVerifyProvider("a product event update")
  public MessageAndMetadata verifyMessageForOrder() {
    ProductEvent product = new ProductEvent("id1", "product name", "product type", "v1", EventType.CREATED);
    Message<String> message = new ProductMessageBuilder().withProduct(product).build();

    return generateMessageAndMetadata(message);
  }

  private MessageAndMetadata generateMessageAndMetadata(Message<String> message) {
    HashMap<String, Object> metadata = new HashMap<String, Object>();
    message.getHeaders().forEach((k, v) -> metadata.put(k, v));

    return new MessageAndMetadata(message.getPayload().getBytes(), metadata);
  }
```

_NOTE: this requires you to add medadata expections in your consumer test_

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

### Provider state teardown methods

If you need to tear down your provider state, you can annotate a method with the `@State` annotation with the action
set to `StateChangeAction.TEARDOWN` and it will be invoked after the interaction is verified.

```java
@State("SomeProviderState", action = StateChangeAction.TEARDOWN)
public void someProviderStateCleanup() {
  // Do what you need to to teardown the state
}
```

#### Returning values that can be injected

You can have values from the provider state callbacks be injected into most places (paths, query parameters, headers,
bodies, etc.). This works by using the V3 spec generators with provider state callbacks that return values. One example
of where this would be useful is API calls that require an ID which would be auto-generated by the database on the
provider side, so there is no way to know what the ID would be beforehand.

There are methods on the consumer DSLs that can provider an expression that contains variables (like '/api/user/${id}'
for the path). The provider state callback can then return a map for values, and the `id` attribute from the map will
be expanded in the expression. For this to work, just make your provider state method return a Map of the values.

### Using multiple classes for the state change methods

If you have a large number of state change methods, you can split things up by moving them to other classes. There are
two ways you can do this:

#### Use interfaces

You can put the state change methods on interfaces and then have your test class implement those interfaces. 
See [StateAnnotationsOnInterfaceTest](https://github.com/DiUS/pact-jvm/blob/master/provider/junit/src/test/java/au/com/dius/pact/provider/junit/StateAnnotationsOnInterfaceTest.java)
for an example.

#### Specify the additional classes on the test target

You can provide the additional classes to the test target with the `withStateHandler` or `setStateHandlers` methods. See
[BooksPactProviderTest](https://github.com/DiUS/pact-jvm/blob/master/provider/spring/src/test/java/au/com/dius/pact/provider/spring/BooksPactProviderTest.java) for an example. 

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
* `pactbroker.auth.username` (for basic auth)
* `pactbroker.auth.password` (for basic auth)
* `pactbroker.auth.token` (for bearer auth)
* `pactbroker.consumers` (comma separated list to filter pacts by consumer; if not provided, will fetch all pacts for the provider)


#### Using tags with the pact broker

The pact broker allows different versions to be tagged. To load all the pacts:

```java
@PactBroker(host="pactbroker", port = "80", tags = {"latest", "dev", "prod"})
```

The default value for tags is `latest` which is not actually a tag but instead corresponds to the latest version ignoring the tags. If there are multiple consumers matching the name specified in the provider annotation then the latest pact for each of the consumers is loaded.

For any other value the latest pact tagged with the specified tag is loaded.

Specifying multiple tags is an OR operation. For example if you specify `tags = {"dev", "prod"}` then both the latest pact file tagged with `dev` and the latest pact file taggged with `prod` is loaded.

In 4.1.4+, tags was deprecated in favor of consumerVersionSelectors. Consumer version selectors give you the ability to 
include pacts for the latest version of a tag, or all versions of a tag.

```java
@PactBroker(
  host="pactbroker", 
  port="80", 
  consumerVersionSelectors={
    @ConsumerVersionSelector(tag = "dev"), // Verify the latest version tagged with dev
    @ConsumerVersionSelector(tag = "prod", latest = "false") // Verify all versions tagged with prod
  }
)
```

#### Using authentication with the pact broker

You can use basic authentication with the `@PactBroker` annotation by setting the `authentication` value to a `@PactBrokerAuth`
annotation. For example:

```java
@PactBroker(host = "${pactbroker.url:localhost}", port = "1234", tags = {"latest", "prod", "dev"},
  authentication = @PactBrokerAuth(username = "test", password = "test"))
```

Bearer tokens are also supported. For example:

```java
@PactBroker(host = "${pactbroker.url:localhost}", port = "1234", tags = {"latest", "prod", "dev"},
  authentication = @PactBrokerAuth(token = "test"))
```

The `token`, `username` and `password` values also take Java system property expressions.

Preemptive Authentication can be enabled by setting the `pact.pactbroker.httpclient.usePreemptiveAuthentication` Java
system property to `true`.

### Allowing just the changed pact specified in a webhook to be verified [4.0.6+]

When a consumer publishes a new version of a pact file, the Pact broker can fire off a webhook with the URL of the changed 
pact file. To allow only the changed pact file to be verified, you can override the URL by adding the annotation 
`@AllowOverridePactUrl` to your test class and then setting using the `pact.filter.consumers` and `pact.filter.pacturl` 
values as either Java system properties or environment variables. If you have annotated your test class with `@Consumer`
you don't need to provide `pact.filter.consumers`.

### Pact Url

To use pacts from urls annotate the test class with

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"})
```

If you need to load a single pact file from the file system, you can use the `PactUrl` with the URL set to the file path.

For authenticated URLs, specify the authentication on the annotation

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"}, authentication = @Authentication(token = "1234ABCD"))
```

You can use either bearer token scheme (by setting the `token`), or basic auth by setting the `username` and `password`.

JVM system properties or environment variables can also be used by placing the property/variable name in `${}` expressions.

```java
@PactUrl(urls = {"http://build.server/zoo_app-animal_service.json"}, authentication = @Authentication(token = "${TOKEN}"))
```

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

#### Interaction Filtering

You can filter the interactions that are executed by adding a `@PactFilter` annotation to your test class. The pact 
filter annotation will then only verify interactions that have a matching value, by default provider state.
You can provide multiple values to match with.

The filter criteria is defined by the filter property. The filter must implement the
`au.com.dius.pact.provider.junit.filter.InteractionFilter` interface. Also check the `InteractionFilter` interface
for default filter implementations.

For example: 

```java
@RunWith(PactRunner.class)
@PactFilter("Activity 100 exists in the database")
public class PactJUnitTest {

}
```

You can also use regular expressions with the filter. For example:

```java
@RunWith(PactRunner.class)
@PactFilter(values = {"^\\/somepath.*"}, filter = InteractionFilter.ByRequestPath.class)
public class PactJUnitTest {

}
```

**NOTE!** You will only be able to publish the verification results if all interactions have been verified. If an interaction is not covered because it was filtered out, you will not be able to publish.

##### Filtering the interactions that are run

**(version 4.1.2+)**

You can filter the interactions that are run by setting the JVM system property `pact.filter.description`. This propery
takes a regular expression to match against the interaction description.

**NOTE!** this property needs to be set on the test JVM if your build is running with Gradle or Maven. 

### Setting the test to not fail when no pacts are found

By default the pact runner will fail the verification test if no pact files are found to verify. To change the
failure into a warning, add a `@IgnoreNoPactsToVerify` annotation to your test class.

#### Ignoring IO errors loading pact files

You can also set the test to ignore any IO and parser exceptions when loading the pact files by setting the
`ignoreIoErrors` attribute on the annotation to `"true"` or setting the JVM system property `pact.verification.ignoreIoErrors`
to `true`.

** WARNING! Do not enable this on your CI server, as this could result in your build passing with no providers 
having been verified due to a configuration error. **        

### Overriding the handling of a body data type

**NOTE: version 4.1.3+**

By default, bodies will be handled based on their content types. For binary contents, the bodies will be base64
encoded when written to the Pact file and then decoded again when the file is loaded. You can change this with
an override property: `pact.content_type.override.<TYPE>.<SUBTYPE>=text|json|binary`. For instance, setting 
`pact.content_type.override.application.pdf=text` will treat PDF bodies as a text type and not encode/decode them.

### Controlling the generation of diffs

**NOTE: version 4.2.7+**

When there are mismatches with large bodies the calculation of the diff can take a long time . You can turn off the 
generation of the diffs with the JVM system property: `pact.verifier.generateDiff=true|false|<dataSize>`, where 
`dataSize`, if specified, must be a valid data size (for instance `100kb` or `1mb`). This will turn off the diff
calculation for payloads that exceed this size. 

For instance, setting `pact.verifier.generateDiff=false` will turn off the generation of diffs for all bodies, while
`pact.verifier.generateDiff=512kb` will only turn off the diffs if the actual or expected body is larger than 512kb.

## Test target

The field in test class of type `au.com.dius.pact.provider.junit.target.Target` annotated with `au.com.dius.pact.provider.junit.target.TestTarget`
will be used for actual Interaction execution and asserting of contract.

**Note:** there must be exactly 1 such field, otherwise an `InitializationException` will be thrown.

### HttpTarget

`au.com.dius.pact.provider.junit.target.HttpTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as http request and assert response from service by matching rules from pact.

You can also specify the protocol, defaults to "http".

### MessageTarget

`au.com.dius.pact.provider.junit.target.MessageTarget` - out-of-the-box implementation of `au.com.dius.pact.provider.junit.target.Target`
that will play pacts as an message and assert response from service by matching rules from pact.

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

**NOTE: `@TargetRequestFilter` is only for JUnit 4. For JUnit 5 see [JUnit 5 docs](/provider/junit5/README.md#modifying-the-requests-before-they-are-sent).**

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The HttpTarget supports request filters by annotating methods
on the test class with `@TargetRequestFilter`. These methods must be public void methods that take a single HttpRequest
parameter of type `org.apache.http.HttpRequest`.

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
 
To enable publishing of results, set the Java system property or environment variable `pact.verifier.publishResults` to `true`.

## Tagging the provider before verification results are published [4.0.1+]

You can have a tag pushed against the provider version before the verification results are published. To do this 
you need set the `pact.provider.tag` JVM system property to the tag value.

From 4.1.8+, you can specify multiple tags with a comma separated string for the `pact.provider.tag`
system property.

## Setting the provider branch before verification results are published [4.3.0-beta.7+]

Pact Broker version 2.86.0 or later

You can have a branch pushed against the provider version before the verification results are published. To do this
you need set the `pact.provider.branch` JVM system property to the branch value.

# Pending Pact Support (version 4.1.3 and later)

If your Pact broker supports pending pacts, you can enable support for that by enabling that on your Pact broker annotation or with JVM system properties. You also need to provide the tags that will be published with your provider's verification results. The broker will then label any pacts found that don't have a successful verification result as pending. That way, if they fail verification, the verifier will ignore those failures and not fail the build.

For example, with annotation:

```java
@Provider("Activity Service")
@PactBroker(host = "test.pactflow.io", tags = {"test"}, scheme = "https",
  enablePendingPacts = "true",
  providerTags = "master"
)
public class PactJUnitTest {
```

You can also use the `pactbroker.enablePending` and `pactbroker.providerTags` JVM system properties. 

Then any pending pacts will not cause a build failure.

# Work In Progress (WIP) Pact Support (version 4.1.5 and later)

If your Pact broker supports wip pacts, you can enable support by enabling it on your Pact broker annotation, or with
JVM system properties. You also need to enable pending pacts. Once enabled, your provider will verify any "work in progress" 
pacts that have been published since a given date. A WIP pact is a pact that is the latest for its tag that does not have 
any successful verification results with the provider tag. 

```java
@Provider("Activity Service")
@PactBroker(host = "test.pactflow.io", tags = {"test"}, scheme = "https",
  enablePendingPacts = "true",
  providerTags = "master"
  includeWipPactsSince = "2020-06-19"
)
public class PactJUnitTest {
```

You can also use the `pactbroker.includeWipPactsSince` JVM system property.

Since all WIP pacts are also pending pacts, failed verifications will not cause a build failure.
