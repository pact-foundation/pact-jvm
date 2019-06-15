package au.com.dius.pact.consumer.v3;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.messaging.MessagePact;


public class ExampleMessageConsumerTest {

    @Rule
    public MessagePactProviderRule mockProvider = new MessagePactProviderRule(this);
    private byte[] currentMessage;

    @Pact(provider = "test_provider", consumer = "test_consumer_v3")
    public MessagePact createPact(MessagePactBuilder builder) {
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

    @Test
    @PactVerification({"test_provider", "SomeProviderState"})
    public void test() throws Exception {
        Assert.assertNotNull(new String(currentMessage));
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
