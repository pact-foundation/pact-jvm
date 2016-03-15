package au.com.dius.pact.consumer.v3;

import au.com.dius.pact.consumer.*;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.exampleclients.ConsumerClient;
import au.com.dius.pact.model.PactConfig;
import au.com.dius.pact.model.PactFragment;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PactVerificationsTest {

    private static final String HTTP_PROVIDER_NAME = "a_http_provider";
    private static final String MESSAGE_PROVIDER_NAME = "a_message_provider";
    private static final String PACT_VERIFICATIONS_CONSUMER_NAME = "pact_verifications_consumer";

    @Rule
    public PactProviderRule httpProvider =
            new PactProviderRule(HTTP_PROVIDER_NAME, "localhost", 8075, new PactConfig(PactSpecVersion.V3), this);

    @Rule
    public MessagePactProviderRule messageProvider = new MessagePactProviderRule(MESSAGE_PROVIDER_NAME, this);

    @Pact(provider = HTTP_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public PactFragment httpPact(PactDslWithProvider builder) {
        return builder
                .given("good state")
                .uponReceiving("PactProviderTest test interaction 2")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body("{\"responsetest\": true, \"name\": \"harry\"}")
                .toFragment();
    }

    @Pact(provider = MESSAGE_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public MessagePact messagePact(MessagePactBuilder builder) {
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
    @PactVerifications({@PactVerification(HTTP_PROVIDER_NAME), @PactVerification(MESSAGE_PROVIDER_NAME)})
    public void shouldTestHttpAndMessagePacts() throws Exception {
        byte[] message = messageProvider.getMessage();
        assertNotNull(message);

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("responsetest", true);
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(httpProvider.getConfig().url()).getAsMap("/", ""), expectedResponse);
    }
}
