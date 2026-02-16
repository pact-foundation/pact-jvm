package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreMissingStateChange;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("parent-provider")
@PactFolder("amqp_pacts")
@IgnoreMissingStateChange
@Disabled("Parent test is expected to fail")
public class ParentProviderTest {
  String event = "{\"id\":\"invalid-uuid\"}";

  public ParentProviderTest() {
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  static class ChildProviderTest extends ParentProviderTest {
    public ChildProviderTest() {
      super();
      event = "{\"id\":\"12341234-1234-1234-1234-123412341234\"}";
    }
  }

  @BeforeEach
  void beforeEach(PactVerificationContext context) {
    context.setTarget(new MessageTestTarget());
  }

  @PactVerifyProvider("an event")
  String anEvent() {
    return event;
  }
}
