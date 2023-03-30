package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.v4.MessageContents;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("KafkaRequestReplyProvider")
@PactFolder("src/test/resources/amqp_pacts")
class SynchronousMessageContractTest {
  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void testTemplate(Pact pact, Interaction interaction, PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MessageTestTarget());
  }

  @PactVerifyProvider("a test message")
  public String verifyMessageForOrder(MessageContents request) {
    return "{\"name\": \"Fred\", \"testParam1\": \"value1\",\"testParam2\": \"value2\"}";
  }
}
