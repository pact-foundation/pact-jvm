# Pact Spring/JUnit5 Support

This module extends the base [Pact JUnit5 module](/provider/junit5/README.md). See that for more details.

## Dependency
The combined library (JUnit5 + Spring) is available on maven central using:

group-id = au.com.dius.pact.provider
artifact-id = junit5spring
version-id = 4.2.x

## Usage
For writing Spring Pact verification tests with JUnit 5, there is an JUnit 5 Invocation Context Provider that you can use with 
the `@TestTemplate` annotation. This will generate a test for each interaction found for the pact files for the provider.

To use it, add the `@Provider` and `@ExtendWith(SpringExtension.class)` and one of the pact source annotations to your test class (as per a JUnit 5 test), then
add a method annotated with `@TestTemplate` and `@ExtendWith(PactVerificationSpringProvider.class)` that
takes a `PactVerificationContext` parameter. You will need to call `verifyInteraction()` on the context parameter in
your test template method.

For example:

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Provider("Animal Profile Service")
@PactBroker
public class ContractVerificationTest {

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

}
```

You will now be able to setup all the required properties using the Spring context, e.g. creating an application
YAML file in the test resources:

```yaml
pactbroker:
  host: your.broker.host
  auth:
    username: broker-user
    password: broker.password
```

You can also run pact tests against `MockMvc` without need to spin up the whole application context which takes time 
and often requires more additional setup (e.g. database). In order to run lightweight tests just use `@WebMvcTest` 
from Spring and `MockMvcTestTarget` as a test target before each test. 

For example:
```java
@WebMvcTest
@Provider("myAwesomeService")
@PactBroker
class ContractVerificationTest {
    
    @Autowired
    private MockMvc mockMvc;

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }
    
    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }
}
```

You can also use `MockMvcTestTarget` for tests without spring context by providing the controllers manually. 

For example:
```java
@Provider("myAwesomeService")
@PactFolder("pacts")
class MockMvcTestTargetStandaloneMockMvcTestJava {

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new DataResource());
        context.setTarget(testTarget);
    }

    @RestController
    static class DataResource {
        @GetMapping("/data")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void getData(@RequestParam("ticketId") String ticketId) {
        }
    }
}
```

**Important:** Since `@WebMvcTest` starts only Spring MVC components you can't use `PactVerificationSpringProvider` 
and need to fallback to `PactVerificationInvocationContextProvider`

## Webflux tests

You can test Webflux routing functions using the `WebFluxTarget` target class. The easiest way to do it is to get Spring to
autowire your handler and router into the test and then pass the routing function to the target.

For example:

```java
  @Autowired
  YourRouter router;

  @Autowired
  YourHandler handler;

  @BeforeEach
  void setup(PactVerificationContext context) {
    context.setTarget(new WebFluxTarget(router.route(handler)));
  }

  @TestTemplate
  @ExtendWith(PactVerificationSpringProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }
```

## Modifying requests

As documented in [Pact JUnit5 module](/provider/junit5/README.md#modifying-the-requests-before-they-are-sent), you can
inject a request object to modify the requests made. However, depending on the Pact test target you are using,
you need to use a different class.

| Test Target | Class to use |
|-------------|--------------|
| HttpTarget, HttpsTarget, SpringBootHttpTarget | org.apache.http.HttpRequest |
| MockMvcTestTarget | MockHttpServletRequestBuilder |
| WebFluxTarget | WebTestClient.RequestHeadersSpec |


# Verifying V4 Pact files that require plugins (version 4.3.0+)

Pact files that require plugins can be verified with version 4.3.0+. For details on how plugins work, see the
[Pact plugin project](https://github.com/pact-foundation/pact-plugins).

Each required plugin is defined in the `plugins` section in the Pact metadata in the Pact file. The plugins will be
loaded from the plugin directory. By default, this is `~/.pact/plugins` or the value of the `PACT_PLUGIN_DIR` environment
variable. Each plugin required by the Pact file must be installed there. You will need to follow the installation
instructions for each plugin, but the default is to unpack the plugin into a sub-directory `<plugin-name>-<plugin-version>`
(i.e., for the Protobuf plugin 0.0.0 it will be `protobuf-0.0.0`). The plugin manifest file must be present for the
plugin to be able to be loaded.

# Test Analytics

We are tracking anonymous analytics to gather important usage statistics like JVM version
and operating system. To disable tracking, set the 'pact_do_not_track' system property or environment
variable to 'true'.
