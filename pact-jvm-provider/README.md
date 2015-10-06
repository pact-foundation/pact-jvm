Pact provider
=============

sub project of https://github.com/DiUS/pact-jvm

The pact provider is responsible for verifying that an API provider adheres to a number of pacts authored by its clients

This library provides the basic tools required to automate the process, and should be usable on its own in many instances.

Framework and build tool specific bindings will be provided in separate libraries that build on top of this core functionality.

### Running Pacts

Main takes 2 arguments:

The first is the root folder of your pact files
(all .json files in root and subfolders are assumed to be pacts)

The second is the location of your pact config json file.

### Pact config


The pact config is a simple mapping of provider names to endpoint url's
paths will be appended to endpoint url's when interactions are attempted

for an example see: https://github.com/DiUS/pact-jvm/blob/master/pact-jvm-provider/src/test/resources/pact-config.json

### Provider State

Before each interaction is executed, the provider under test will have the opportunity to enter a state.
Generally the state maps to a set of fixture data for mocking out services that the provider is a consumer of (they will have their own pacts)

The pact framework will instruct the test server to enter that state by sending:

    POST "${config.stateChangeUrl.url}/setup" { "state" : "${interaction.stateName}" }


### An example of running provider verification with junit

This example uses java, junit and hamcrest matchers to run the provider verification. 
As the provider service is a DropWizard application, it uses the DropwizardAppRule to startup the service before running any test.

Warning: It only grabs the first interaction from the pact file with the consumer, where there could be many. (This could possibly be solved with a parameterized test)

```java
public class PactJVMProviderJUnitTest {

    @ClassRule
    public static TestRule startServiceRule = new DropwizardAppRule<DropwizardAppConfig>(DropwizardApp.class, "config.yml");

    private static ProviderInfo serviceProvider;
    private static Pact testConsumerPact;
    
    @BeforeClass
    public static void setupProvider() {
        serviceProvider = new ProviderInfo("Dropwizard App");
        serviceProvider.setProtocol("http");
        serviceProvider.setHost("localhost");
        serviceProvider.setPort(8080);
        serviceProvider.setPath("/");
        
        ConsumerInfo consumer = new ConsumerInfo();
        consumer.setName("test_consumer");
        consumer.setPactFile(new File("target/pacts/ping_client-ping_service.json"));
        
    //  serviceProvider.getConsumers().add(consumer);
        testConsumerPact = (Pact) new PactReader().loadPact(consumer.getPactFile());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void runConsumerPacts() {

        //grab the first interaction from the pact with consumer
        List<Interaction> interactions = scala.collection.JavaConversions.seqAsJavaList(testConsumerPact.interactions());
        Interaction interaction1 = interactions.get(0);
        
        //setup any provider state

        //setup the client and interaction to fire against the provider
        ProviderClient client = new ProviderClient();
        client.setProvider(serviceProvider);
        client.setRequest(interaction1.request());
        Map<String, Object> clientResponse = (Map<String, Object>) client.makeRequest();
        Map<String, Object> result = (Map<String, Object>) ResponseComparison.compareResponse(interaction1.response(), 
                clientResponse, (int) clientResponse.get("statusCode"), (Map) clientResponse.get("headers"), (String) clientResponse.get("data"));

        //assert all good
        assertThat(result.get("method"), is(true)); // method type matches
        
        Map headers = (Map) result.get("headers"); //headers match
        headers.forEach( (k, v) -> 
            assertThat(format("Header: [%s] does not match", k), v, org.hamcrest.Matchers.equalTo(true))
        );
        
        assertThat((Collection<Object>)((Map)result.get("body")).values(), org.hamcrest.Matchers.hasSize(0)); // empty list of body mismatches
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
    
