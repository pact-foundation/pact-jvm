package au.com.dius.pact.provider.junit;

import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(PactRunner.class)
@Provider("providerWithMultipleInteractions")
@PactFolder("pacts")
public class StateAnnotationsOnInterfaceTest implements StateInterface1, StateInterface2 {

  @ClassRule
  public static final ClientDriverRule embeddedProvider = new ClientDriverRule(8333);

  public ClientDriverRule embeddedProvider() {
    return embeddedProvider;
  }

  @TestTarget
  public final Target target = new HttpTarget(8333);

}
