# Pact Spring/JUnit runner

## Overview
Library provides ability to play contract tests against a provider using Spring & JUnit.
This library is based on and references the JUnit package, so see the [Pact JUnit 4](/provider/junit/README.md) or [Pact JUnit 5](/provider/junit5/README.md) providers for more details regarding configuration using JUnit.

Supports:

- Standard ways to load pacts from folders and broker

- Easy way to change assertion strategy

- Spring Test MockMVC Controllers and ControllerAdvice using MockMvc standalone setup.

- MockMvc debugger output

- Multiple @State runs to test a particular Provider State multiple times

- **au.com.dius.pact.provider.junit.State** custom annotation - before each interaction that requires a state change,
all methods annotated by `@State` with appropriate the state listed will be invoked.

**NOTE:** For publishing provider verification results to a pact broker, make sure the Java system property `pact.provider.version`
is set with the version of your provider. 

## Example of MockMvc test

```java
    @RunWith(RestPactRunner.class) // Custom pact runner, child of PactRunner which runs only REST tests
    @Provider("myAwesomeService") // Set up name of tested provider
    @PactFolder("pacts") // Point where to find pacts (See also section Pacts source in documentation)
    public class ContractTest {
        //Create an instance of your controller.  We cannot autowire this as we're not using (and don't want to use)  a Spring test runner.
        @InjectMocks
        private AwesomeController awesomeController = new AwesomeController();

        //Mock your service logic class.  We'll use this to create scenarios for respective provider states.
        @Mock
        private AwesomeBusinessLogic awesomeBusinessLogic;

        //Create an instance of your controller advice (if you have one).  This will be passed to the MockMvcTarget constructor to be wired up with MockMvc.
        @InjectMocks
        private AwesomeControllerAdvice awesomeControllerAdvice = new AwesomeControllerAdvice();

        //Create a new instance of the MockMvcTarget and annotate it as the TestTarget for PactRunner
        @TestTarget
        public final MockMvcTarget target = new MockMvcTarget();

        @Before //Method will be run before each test of interaction
        public void before() {
            //initialize your mocks using your mocking framework
            MockitoAnnotations.initMocks(this);

            //configure the MockMvcTarget with your controller and controller advice
            target.setControllers(awesomeController);
            target.setControllerAdvice(awesomeControllerAdvice);
        }

        @State("default", "no-data") // Method will be run before testing interactions that require "default" or "no-data" state
        public void toDefaultState() {
            target.setRunTimes(3);  //let's loop through this state a few times for a 3 data variants
            when(awesomeBusinessLogic.getById(any(UUID.class)))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.ONE))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.TWO))
                .thenReturn(myTestHelper.generateRandomReturnData(UUID.randomUUID(), ExampleEnum.THREE));
        }

        @State("error-case")
        public void SingleUploadExistsState_Success() {
            target.setRunTimes(1); //tell the runner to only loop one time for this state
            
            //you might want to throw exceptions to be picked off by your controller advice
            when(awesomeBusinessLogic.getById(any(UUID.class)))
                .then(i -> { throw new NotCoolException(i.getArgumentAt(0, UUID.class).toString()); });
        }
    }
```

## Using Spring runners

You can use `SpringRestPactRunner` or `SpringMessagePactRunner` instead of the default Pact runner to use the Spring test annotations. This will
allow you to inject or mock spring beans. `SpringRestPactRunner` is for restful webapps and `SpringMessagePactRunner` is
for async message tests.

For example:

```java
@RunWith(SpringRestPactRunner.class)
@Provider("pricing")
@PactBroker(protocol = "https", host = "${pactBrokerHost}", port = "443",
authentication = @PactBrokerAuth(username = "${pactBrokerUser}", password = "${pactBrokerPassword}"))
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class PricingServiceProviderPactTest {

  @MockBean
  private ProductClient productClient;  // This will replace the bean with a mock in the application context

  @TestTarget
  @SuppressWarnings(value = "VisibilityModifier")
  public final Target target = new HttpTarget(8091);

  @State("Product X010000021 exists")
  public void setupProductX010000021() throws IOException {
    reset(productClient);
    ProductBuilder product = new ProductBuilder()
      .withProductCode("X010000021");
    when(productClient.fetch((Set<String>) argThat(contains("X010000021")), any())).thenReturn(product);
  }

  @State("the product code X00001 can be priced")
  public void theProductCodeX00001CanBePriced() throws IOException {
    reset(productClient);
    ProductBuilder product = new ProductBuilder()
      .withProductCode("X00001");
    when(productClient.find((Set<String>) argThat(contains("X00001")), any())).thenReturn(product);
  }

}
```

### Using Spring Context Properties

The SpringRestPactRunner will look up any annotation expressions (like `${pactBrokerHost}`)
above) from the Spring context. For Springboot, this will allow you to define the properties in the application test properties.

For instance, if you create the following `application.yml` in the test resources:

```yaml
pactbroker:
  host: "your.broker.local"
  port: "443"
  protocol: "https"
  auth:
    username: "<your broker username>"
    password: "<your broker password>"

```

Then you can use the defaults on the `@PactBroker` annotation.

```java
@RunWith(SpringRestPactRunner.class)
@Provider("My Service")
@PactBroker(
  authentication =  @PactBrokerAuth(username = "${pactbroker.auth.username}", password = "${pactbroker.auth.password}")
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PactVerificationTest {

```

### Using a random port with a Springboot test
If you use a random port in a springboot test (by setting `SpringBootTest.WebEnvironment.RANDOM_PORT`), you need to set it to the `TestTarget`. How this works is different for JUnit4 and JUnit5.

#### JUnit4
You can use the
`SpringBootHttpTarget` which will get the application port from the spring application context.

For example:

```java
@RunWith(SpringRestPactRunner.class)
@Provider("My Service")
@PactBroker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PactVerificationTest {

  @TestTarget
  public final Target target = new SpringBootHttpTarget();

}
```

#### JUnit5
You actually don't need to dependend on `pact-jvm-provider-spring` for this. It's sufficient to depend on `pact-jvm-provider-junit5`. 

You can set the port to the `HttpTestTarget` object in the before method.

```java
@Provider("My Service")
@PactBroker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PactVerificationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

}
```


