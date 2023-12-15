package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.Matchers;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "AmqpProviderWithMetadata", providerType = ProviderType.ASYNCH)
public class AsyncMessageWithMetadataTest {

  @Pact(consumer = "test_consumer")
  public V4Pact withMetadata(MessagePactBuilder builder) {
    return builder
            .given("Some State")
            .expectsToReceive("A message with metadata")
            .withMetadata(Map.of("someKey", Matchers.string("someString")))
            .withContent(new PactDslJsonBody().stringType("someField", "someValue"))
            .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor
  void test(List<V4Interaction.AsynchronousMessage> messages) {
    assertThat(new String(messages.get(0).contentsAsBytes()), is("{\"someField\":\"someValue\"}"));
    assertThat(messages.get(0).getMetadata(), hasEntry("someKey", "someString"));
  }
}
