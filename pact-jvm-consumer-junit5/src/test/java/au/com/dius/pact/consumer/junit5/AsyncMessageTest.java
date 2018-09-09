package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "MessageProvider", providerType = ProviderType.ASYNCH)
public class AsyncMessageTest {

  @Pact(consumer = "test_consumer_v3")
  MessagePact createPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringValue("testParam1", "value1");
    body.stringValue("testParam2", "value2");

    Map<String, String> metadata = new HashMap<String, String>();
    metadata.put("contentType", "application/json");

    return builder.given("SomeProviderState")
      .expectsToReceive("a test message")
      .withMetadata(metadata)
      .withContent(body)
      .toPact();
  }

  @Pact(provider = "MessageProvider", consumer = "test_consumer_v3")
  MessagePact createPact2(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringValue("testParam1", "value3");
    body.stringValue("testParam2", "value4");

    Map<String, String> metadata = new HashMap<String, String>();
    metadata.put("contentType", "application/json");

    return builder.given("SomeProviderState2")
      .expectsToReceive("a test message")
      .withMetadata(metadata)
      .withContent(body)
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createPact")
  void test(List<Message> messages) {
    assertThat(new String(messages.get(0).contentsAsBytes()), is("{\"testParam1\":\"value1\",\"testParam2\":\"value2\"}"));
  }

  @Test
  @PactTestFor(pactMethod = "createPact2")
  void test2(List<Message> messages) {
    assertThat(new String(messages.get(0).contentsAsBytes()), is("{\"testParam1\":\"value3\",\"testParam2\":\"value4\"}"));
  }
}
