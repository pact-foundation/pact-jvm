package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@Provider("ProviderStateParametersInjected")
@PactFolder("pacts")
@RunWith(PactRunner.class)
public class ProviderStateParametersInjectedTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProviderStateParametersInjectedTest.class);

  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(9241);

  @TestTarget
  public final Target target = new HttpTarget(9241);

  @Before
  public void before() {
    embeddedService.addExpectation(
      onRequestTo("/api/hello/John"),
      giveResponse("{\"name\": \"John\"}", "application/json")
    );
  }

  @State("User exists")
  public Map<String, Object> defaultState(Map<String, Object> params) {
    LOGGER.debug("Provider state params = " + params);
    return Collections.emptyMap();
  }
}
