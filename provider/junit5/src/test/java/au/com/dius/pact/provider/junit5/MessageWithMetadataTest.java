package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@Provider("AmqpProviderWithMetadata")
@PactFolder("src/test/resources/amqp_pacts")
class MessageWithMetadataTest {
  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context) {
    context.setTarget(new MessageTestTarget());
  }

  @PactVerifyProvider("A message with metadata")
  public MessageAndMetadata verifyV4MessageWithMetadataForOrder() {
    return new MessageAndMetadata(
            "{\"someField\": \"someValue\"}".getBytes(),
            Map.of("someKey", "different string pact but same type")
    );
  }
}
