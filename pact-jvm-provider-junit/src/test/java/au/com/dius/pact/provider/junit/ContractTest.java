package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("myAwesomeService")
@PactFolder("pacts")
public class ContractTest {
    // NOTE: this is just an example of embedded service that listens to requests, you should start here real service
    @ClassRule
    public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);

    @BeforeClass
    public static void setUpService() {
        //Run DB, create schema
        //Run service
        //...
    }

    @Before
    public void before() {
        // Rest data
        // Mock dependent service responses
        // ...
        embeddedService.addExpectation(
                onRequestTo("/data"), giveEmptyResponse()
        );
    }

    @State("default")
    public void toDefaultState() {
        // Prepare service before interaction that require "default" state
        // ...
        System.out.println("Now service in default state");
    }

    @TestTarget
    public final Target target = new HttpTarget(8332);
}
