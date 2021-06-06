package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("test_provider")
@PactFolder("pacts")
public class V4PendingInteractionTest {
  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);

  @TestTarget
  public final Target target = new HttpTarget(8332);

  @Before
  public void before() {
    embeddedService.addExpectation(
      onRequestTo("/data").withAnyParams(), giveResponse("{}", "application/json")
    );
  }
}
