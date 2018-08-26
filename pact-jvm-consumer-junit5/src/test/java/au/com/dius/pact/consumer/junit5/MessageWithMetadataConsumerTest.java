package au.com.dius.pact.consumer.junit5;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.model.v3.messaging.Message;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "MessageProvider", providerType = ProviderType.ASYNCH)
public class MessageWithMetadataConsumerTest {

    @Pact(provider = "test_provider", consumer = "test_consumer_v3")
    public MessagePact createPact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("testParam1", "value1");
        body.stringValue("testParam2", "value2");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("metadata1", "metadataValue1");
        metadata.put("metadata2", "metadataValue2");

        return builder.given("SomeProviderState")
                .expectsToReceive("a test message with metadata")
                .withMetadata(metadata)
                .withContent(body)
                .toPact();
    }

    @Test
    void test(List<Message> messages) {
        assertTrue(!messages.isEmpty());
        assertTrue(!messages.get(0).getMetaData().isEmpty());
        assertEquals("metadataValue1", messages.get(0).getMetaData().get("metadata1"));
        assertEquals("metadataValue2", messages.get(0).getMetaData().get("metadata2"));
    }

}
