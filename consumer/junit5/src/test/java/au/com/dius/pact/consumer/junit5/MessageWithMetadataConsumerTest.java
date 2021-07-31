package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.matchingrules.RegexMatcher;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "MessageProvider", providerType = ProviderType.ASYNCH)
public class MessageWithMetadataConsumerTest {

  @Pact(consumer = "test_consumer_v3")
  public MessagePact createPact(MessagePactBuilder builder) {
    PactDslJsonBody body = new PactDslJsonBody();
    body.stringValue("testParam1", "value1");
    body.stringValue("testParam2", "value2");

    return builder.given("SomeProviderState")
      .expectsToReceive("a test message with metadata")
      .withMetadata(md -> {
        md.add("metadata1", "metadataValue1");
        md.add("metadata2", "metadataValue2");
        md.add("metadata3", 10L);
        md.matchRegex("partitionKey", "[A-Z]{3}\\d{2}", "ABC01");
      })
      .withContent(body)
      .toPact();
  }

  @Test
  void test(List<Message> messages) {
    assertThat(messages, is(not(empty())));
    Message message = messages.get(0);
    Map<String, Object> metaData = message.getMetaData();
    assertThat(metaData.entrySet(), is(not(empty())));
    assertThat(metaData.get("metadata1"), is("metadataValue1"));
    assertThat(metaData.get("metadata2"), is("metadataValue2"));
    assertThat(metaData.get("metadata3"), is(10L));
    assertThat(metaData.get("partitionKey"), is("ABC01"));

    assertThat(message.getMatchingRules().rulesForCategory("metadata").allMatchingRules(),
      is(Collections.singletonList(new RegexMatcher("[A-Z]{3}\\d{2}"))));
  }
}
