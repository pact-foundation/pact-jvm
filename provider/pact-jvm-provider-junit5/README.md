# Pact Junit 5 Extension

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

For details on the provider and pact source annotations, refer to the [Pact junit runner](../pact-jvm-provider-junit/README.md) docs.

## Test target

You can set the test target (the object that defines the target of the test, which should point to your provider) on the
`PactVerificationContext`, but you need to do this in a before test method (annotated with `@BeforeEach`). There are three
different test targets you can use: `HttpTestTarget`, `HttpsTestTarget` and `AmpqTestTarget`.

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

Provider State Methods work in the same way as with JUnit 4 tests, refer to the [Pact junit runner](../pact-jvm-provider-junit/README.md) docs.

## Modifying the requests before they are sent

**Important Note:** You should only use this feature for things that can not be persisted in the pact file. By modifying
 the request, you are potentially modifying the contract from the consumer tests!

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
