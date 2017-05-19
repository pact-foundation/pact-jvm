package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(ExpectedToFailPactRunner.class)
@Provider("myAwesomeService")
@PactFolder("pacts")
@Ignore("Needs an upgrade to ClientDriverRule")
public class ProviderStateTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderStateTest.class);

    @ClassRule
    public static final ClientDriverRule embeddedService = new ClientDriverRule(8333);

    @TestTarget
    public final Target target = new HttpTarget(8333);

    @Before
    public void before() {
//      embeddedService.noFailFastOnUnexpectedRequest();
      embeddedService.addExpectation(
        onRequestTo("/data"), giveEmptyResponse()
      );
    }

    @After
    public void after() {
//      embeddedService.reset();
    }
}
