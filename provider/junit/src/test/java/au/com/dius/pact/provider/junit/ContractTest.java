package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter;
import au.com.dius.pact.provider.junitsupport.VerificationReports;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("myAwesomeService")
@PactFolder("pacts")
@VerificationReports(value = {"console", "json", "markdown"}, reportDir = "build/pacts/reports")
public class ContractTest {
    // NOTE: this is just an example of embedded service that listens to requests, you should start here real service
    @ClassRule
    public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractTest.class);
    @TestTarget
    public final Target target = new HttpTarget(8332);

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
                onRequestTo("/data").withAnyParams(), giveEmptyResponse()
        );
    }

    @State("default")
    public void toDefaultState() {
        // Prepare service before interaction that require "default" state
        // ...
      LOGGER.info("Now service in default state");
    }

    @State("state 2")
    public void toSecondState(Map params) {
        // Prepare service before interaction that require "state 2" state
        // ...
        LOGGER.info("Now service in 'state 2' state: " + params);
    }

    @TargetRequestFilter
    public void exampleRequestFilter(ClassicHttpRequest request) {
      LOGGER.info("exampleRequestFilter called: " + request);
    }
}
