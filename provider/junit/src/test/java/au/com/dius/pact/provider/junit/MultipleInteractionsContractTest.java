package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.restdriver.clientdriver.RestClientDriver.*;

@RunWith(PactRunner.class)
@Provider("providerWithMultipleInteractions")
@PactFolder("src/test/resources/pacts")
@VerificationReports(value = {"console", "json", "markdown"}, reportDir = "build/pacts/reports")
public class MultipleInteractionsContractTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultipleInteractionsContractTest.class);

  @TestTarget
  public final Target target = new HttpTarget(8000);

  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8000);

  @Before
  public void before() {
    embeddedService.reset();
  }

  @State("state1")
  public void stateChange() {
    LOGGER.debug("stateChange - state1");
    embeddedService.addExpectation(
      onRequestTo("/data"), giveResponse("{}", "application/json").withStatus(204)
    );
  }

  @State("state2")
  public void stateChange2() {
    LOGGER.debug("stateChange - state2");
    embeddedService.addExpectation(
      onRequestTo("/moreData"), giveEmptyResponse().withStatus(204)
    );
  }

}
