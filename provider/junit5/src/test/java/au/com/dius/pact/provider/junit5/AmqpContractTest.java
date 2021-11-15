package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider("AmqpProvider")
@PactFolder("src/test/resources/amqp_pacts")
class AmqpContractTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AmqpContractTest.class);

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
    LOGGER.info("testTemplate called: " + pact.getProvider().getName() + ", " + interaction);
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MessageTestTarget());
  }

  @State("SomeProviderState")
  public void someProviderState() {
    LOGGER.info("SomeProviderState callback");
  }

  @PactVerifyProvider("a V3 test message")
  public String verifyMessageForOrder() {
    return "{\"testParam1\": \"value1\",\"testParam2\": \"value2\"}";
  }

  @PactVerifyProvider("a V4 test message")
  public String verifyV4MessageForOrder() {
    return "{\"testParam1\": \"value1\",\"testParam2\": \"value2\"}";
  }
}
