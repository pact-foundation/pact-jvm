package au.com.dius.pact.provider.junit;

import au.com.dius.pact.core.matchers.Matchers;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.StateChangeAction;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("ArticlesProvider")
@PactFolder("src/test/resources/wildcards")
public class ArticlesContractTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArticlesContractTest.class);

  @TestTarget
  public final Target target = new HttpTarget(8000);

  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8000);

  @Before
  public void before() throws IOException {
    System.setProperty(Matchers.PACT_MATCHING_WILDCARD, "true");
    String json = IOUtils.toString(getClass().getResourceAsStream("/articles.json"), Charset.defaultCharset());
    embeddedService.addExpectation(
      onRequestTo("/articles.json"), giveResponse(json, "application/json")
    );
  }

  @After
  public void after() {
    System.clearProperty(Matchers.PACT_MATCHING_WILDCARD);
  }

  @State("Pact for Issue 313")
  public void stateChange() {
    LOGGER.debug("stateChange - Pact for Issue 313 - Before");
  }

  @State(value = "Pact for Issue 313", action = StateChangeAction.TEARDOWN)
  public void stateChangeAfter() {
    LOGGER.debug("stateChange - Pact for Issue 313 - After");
  }
}
