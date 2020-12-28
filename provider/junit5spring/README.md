# Pact Spring/JUnit5 Support

This module extends the base [Pact JUnit5 module](/provider/junit5/README.md). See that for more details.

## Dependency
The combined library (JUnit5 + Spring) is available on maven central using:

group-id = au.com.dius.pact.provider
artifact-id = junit5spring
version-id = 4.1.x

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
    @ExtendWith(PactVerificationInvocationContextProvider.class)
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
    @ExtendWith(PactVerificationInvocationContextProvider.class)
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
