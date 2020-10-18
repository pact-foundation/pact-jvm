package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.VerificationReports;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;

@RunWith(PactRunner.class)
@Provider("xml_provider")
@PactFolder("pacts")
@VerificationReports({"console", "json", "markdown"})
public class XMLContractTest {
  @ClassRule
  public static final ClientDriverRule embeddedService = new ClientDriverRule(8332);

  @TestTarget
  public final Target target = new HttpTarget(8332);

  @Before
  public void before() {
    embeddedService.addExpectation(
      onRequestTo("/attr").withMethod(ClientDriverRequest.Method.POST), giveEmptyResponse()
    );
  }
}
