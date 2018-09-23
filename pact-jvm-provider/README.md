Pact provider
=============

sub project of https://github.com/DiUS/pact-jvm

The pact provider is responsible for verifying that an API provider adheres to a number of pacts authored by its clients

This library provides the basic tools required to automate the process, and should be usable on its own in many instances.

Framework and build tool specific bindings will be provided in separate libraries that build on top of this core functionality.

### Provider State

Before each interaction is executed, the provider under test will have the opportunity to enter a state.
Generally the state maps to a set of fixture data for mocking out services that the provider is a consumer of (they will have their own pacts)

The pact framework will instruct the test server to enter that state by sending:

    POST "${config.stateChangeUrl.url}/setup" { "state" : "${interaction.stateName}" }


### An example of running provider verification with junit

This example uses Groovy, JUnit 4 and Hamcrest matchers to run the provider verification. 
As the provider service is a DropWizard application, it uses the DropwizardAppRule to startup the service before running any test.

**Warning:** It only grabs the first interaction from the pact file with the consumer, where there could be many. (This could possibly be solved with a parameterized test)

```groovy
class ReadmeExamplePactJVMProviderJUnitTest {

  @ClassRule
  public static TestRule startServiceRule = new DropwizardAppRule<DropwizardConfiguration>(
    TestDropwizardApplication.class, ResourceHelpers.resourceFilePath("dropwizard/test-config.yaml"))

  private static ProviderInfo serviceProvider
  private static Pact<RequestResponseInteraction> testConsumerPact
  private static ConsumerInfo consumer

  @BeforeClass
  static void setupProvider() {
    serviceProvider = new ProviderInfo("Dropwizard App")
    serviceProvider.setProtocol("http")
    serviceProvider.setHost("localhost")
    serviceProvider.setPort(8080)
    serviceProvider.setPath("/")

    consumer = new ConsumerInfo()
    consumer.setName("test_consumer")
    consumer.setPactSource(new UrlSource(
      ReadmeExamplePactJVMProviderJUnitTest.getResource("/pacts/zoo_app-animal_service.json").toString()))

    testConsumerPact = PactReader.loadPact(consumer.getPactSource()) as Pact<RequestResponseInteraction>
  }

  @Test
  void runConsumerPacts() {
    // grab the first interaction from the pact with consumer
    Interaction interaction = testConsumerPact.interactions.get(0)

    // setup the verifier
    ProviderVerifier verifier = setupVerifier(interaction, serviceProvider, consumer)

    // setup any provider state

    // setup the client and interaction to fire against the provider
    ProviderClient client = new ProviderClient(serviceProvider, new HttpClientFactory())
    Map<String, Object> failures = new HashMap<>()
    verifier.verifyResponseFromProvider(serviceProvider, interaction, interaction.getDescription(), failures, client)

    if (!failures.isEmpty()) {
      verifier.displayFailures(failures)
    }

    // Assert all good
    assertThat(failures, is(empty()))
  }

  private ProviderVerifier setupVerifier(Interaction interaction, ProviderInfo provider, ConsumerInfo consumer) {
    ProviderVerifier verifier = new ProviderVerifier()

    verifier.initialiseReporters(provider)
    verifier.reportVerificationForConsumer(consumer, provider)

    if (!interaction.getProviderStates().isEmpty()) {
      for (ProviderState providerState: interaction.getProviderStates()) {
        verifier.reportStateForInteraction(providerState.getName(), provider, consumer, true)
      }
    }

    verifier.reportInteractionDescription(interaction)

    return verifier
  }
}
```
    
### An example of running provider verification with spock

This example uses groovy and spock to run the provider verification. 
Again the provider service is a DropWizard application, and is using the DropwizardAppRule to startup the service.

This example runs all interactions using spocks Unroll feature

```groovy
class PactJVMProviderSpockSpec extends Specification {

    @ClassRule @Shared
    TestRule startServiceRule = new DropwizardAppRule<DropwizardAppConfig>(DropwizardApp.class, "config.yml");

    @Shared
    ProviderInfo serviceProvider
    @Shared
    Pact testConsumerPact

    def setupSpec() {
        serviceProvider = new ProviderInfo("Dropwizard App")
        serviceProvider.protocol = "http"
        serviceProvider.host = "localhost"
        serviceProvider.port = 8080;
        serviceProvider.path = "/"
        def consumer = serviceProvider.hasPactWith("ping_consumer", {
            pactFile = new File('target/pacts/ping_client-ping_service.json')
        })

        testConsumerPact = (Pact) new PactReader().loadPact(consumer.getPactFile());
    }

    def cleanup() {
        //cleanup provider state
        //ie. db.truncateAllTables()
    }

    def cleanupSpec() {
        //cleanup provider
    }

    @Unroll
    def "Provider Pact - With Consumer"() {
        given:
        //setup provider state
        // ie.    db.setupRecords()
        //        serviceProvider.requestFilter = { req ->
        //            req.addHeader('Authorization', token)
        //        }

        when:
        ProviderClient client = new ProviderClient(provider: serviceProvider, request: interaction.request())
        Map clientResponse = (Map) client.makeRequest()
        Map result = (Map) ResponseComparison.compareResponse(interaction.response(),
                clientResponse, clientResponse.statusCode, clientResponse.headers, clientResponse.data)

        then:

        // method matches
        result.method == true

        // headers all match, spock needs the size checked before
        // asserting each result
        if (result.headers.size() > 0) {
            result.headers.each() { k, v ->
                assert v == true
            }
        }

        // empty list of body mismatches
        result.body.size() == 0

        where:
        interaction << scala.collection.JavaConversions.seqAsJavaList(testConsumerPact.interactions())
    }
}
```
    
