package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.HttpRequest;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PactRunner.class)
@Provider("myAwesomeService")
@PactFolder("pacts")
public class InjectedStateParametersContractTest {
  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);
  private static final Logger LOGGER = LoggerFactory.getLogger(InjectedStateParametersContractTest.class);

  @TestTarget
  public final Target target = new HttpTarget(8332);

  @Before
  public void before() {
    embeddedService.addExpectation(
      onRequestTo("/data")
        .withHeader("X-ContractTest", equalTo("true"))
        .withParam("ticketId", Pattern.compile("0000|99987")),

      giveEmptyResponse().withHeader("Location", "http://localhost:8332/ticket/1234")
    );
  }

  @TargetRequestFilter
  public void exampleRequestFilter(HttpRequest request) {
    request.addHeader("X-ContractTest", "true");
  }

  @State("default")
  public Map<String, Object> toDefaultState() {
    // Prepare service before interaction that require "default" state
    // ...
    LOGGER.info("Now service in default state");

    return new HashMap<>();
  }

  @State("state 2")
  public Map<String, Object> toSecondState(Map params) {
    // Prepare service before interaction that require "state 2" state
    // ...
    LOGGER.info("Now service in 'state 2' state: " + params);

    HashMap<String, Object> map = new HashMap<>();
    map.put("ticketId", "99987");
    return map;
  }
}
