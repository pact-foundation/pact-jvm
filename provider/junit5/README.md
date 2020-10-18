# Pact Junit 5 Extension

## Dependency

The library is available on maven central using:

* group-id = `au.com.dius.pact.provider`
* artifact-id = `junit5`
* version-id = `4.1.x`

## Overview

For writing Pact verification tests with JUnit 5, there is an JUnit 5 Invocation Context Provider that you can use with 
the `@TestTemplate` annotation. This will generate a test for each interaction found for the pact files for the provider.

To use it, add the `@Provider` and one of the pact source annotations to your test class (as per a JUnit 4 test), then
add a method annotated with `@TestTemplate` and `@ExtendWith(PactVerificationInvocationContextProvider.class)` that
takes a `PactVerificationContext` parameter. You will need to call `verifyInteraction()` on the context parameter in
your test template method.

For example:

```java
@Provider("myAwesomeService")
@PactFolder("pacts")
public class ContractVerificationTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

}
```

For details on the provider and pact source annotations, refer to the [Pact junit runner](../junit/README.md) docs.

## Test target

You can set the test target (the object that defines the target of the test, which should point to your provider) on the
`PactVerificationContext`, but you need to do this in a before test method (annotated with `@BeforeEach`). There are three
different test targets you can use: `HttpTestTarget`, `HttpsTestTarget` and `MessageTestTarget`.

For example:

```java
  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(HttpTestTarget.fromUrl(new URL(myProviderUrl)));
    // or something like
    // context.setTarget(new HttpTestTarget("localhost", myProviderPort, "/"));
  }
```

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

## Provider State Methods

Provider State Methods work in the same way as with JUnit 4 tests, refer to the [Pact junit runner](../junit/README.md) docs.

### Using multiple classes for the state change methods

If you have a large number of state change methods, you can split things up by moving them to other classes. You will 
need to specify the additional classes on the test context in a `Before` method. Do this with the `withStateHandler` 
or `setStateHandlers` methods. See [StateAnnotationsOnAdditionalClassTest](https://github.com/DiUS/pact-jvm/blob/master/provider/junit5/src/test/java/au/com/dius/pact/provider/junit5/StateAnnotationsOnAdditionalClassTest.java) for an example. 

## Modifying the requests before they are sent

**Important Note:** You should only use this feature for things that can not be persisted in the pact file. By modifying
 the request, you are potentially modifying the contract from the consumer tests!
 
**NOTE: JUnit 5 tests do not use `@TargetRequestFilter`**

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would be
authentication tokens, which have a small life span. The Http and Https test targets support injecting the request that
will executed into the test template method.
You can then add things to the request before calling the `verifyInteraction()` method.

For example to add a header:

```java
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(PactVerificationContext context, HttpRequest request) {
      // This will add a header to the request
      request.addHeader("X-Auth-Token", "1234");
      context.verifyInteraction();
    }
```

## Objects that can be injected into the test methods

You can inject the following objects into your test methods (just like the `PactVerificationContext`). They will be null if injected before the
supported phase.

| Object | Can be injected from phase | Description |
| ------ | --------------- | ----------- |
| PactVerificationContext | @BeforeEach | The context to use to execute the interaction test |
| Pact | any | The Pact model for the test |
| Interaction | any | The Interaction model for the test |
| HttpRequest | @TestTemplate | The request that is going to be executed (only for HTTP and HTTPS targets) |
| ProviderVerifier | @TestTemplate | The verifier instance that is used to verify the interaction |

## Allowing the test to pass when no pacts are found to verify (version 4.0.7+)

By default, the test will fail with an exception if no pacts were found to verify. This can be overridden by adding the 
`@IgnoreNoPactsToVerify` annotation to the test class. For this to work, you test class will need to be able to receive 
null values for any of the injected parameters.

## Overriding the handling of a body data type

**NOTE: version 4.1.3+**

By default, bodies will be handled based on their content types. For binary contents, the bodies will be base64
encoded when written to the Pact file and then decoded again when the file is loaded. You can change this with
an override property: `pact.content_type.override.<TYPE>.<SUBTYPE>=text|binary`. For instance, setting 
`pact.content_type.override.application.pdf=text` will treat PDF bodies as a text type and not encode/decode them.

# Pending Pact Support (version 4.1.0 and later)

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

WIP pacts work in the same way as with JUnit 4 tests, refer to the [Pact junit runner](../junit/README.md) docs.
