package au.com.dius.pact.consumer.junit.v3;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit.MessagePactProviderRule;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.consumer.junit.PactVerifications;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.exampleclients.ConsumerClient;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PactVerificationsForMultipleHttpsAndMessagesTest {

    private static final String HTTP_PROVIDER_NAME = "a_http_provider";
    private static final String OTHER_HTTP_PROVIDER_NAME = "other_http_provider";
    private static final String MESSAGE_PROVIDER_NAME = "a_message_provider";
    private static final String OTHER_MESSAGE_PROVIDER_NAME = "other_message_provider";
    private static final String PACT_VERIFICATIONS_CONSUMER_NAME = "pact_verifications_multiple_https_and_messages_consumer";

    @Rule
    public PactProviderRule httpProvider =
            new PactProviderRule(HTTP_PROVIDER_NAME, PactSpecVersion.V3, this);

    @Rule
    public PactProviderRule otherHttpProvider =
            new PactProviderRule(OTHER_HTTP_PROVIDER_NAME, PactSpecVersion.V3, this);

    @Rule
    public MessagePactProviderRule messageProvider = new MessagePactProviderRule(MESSAGE_PROVIDER_NAME, this);

    @Rule
    public MessagePactProviderRule otherMessageProvider = new MessagePactProviderRule(OTHER_MESSAGE_PROVIDER_NAME, this);

    @Pact(provider = HTTP_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public RequestResponsePact httpPact(PactDslWithProvider builder) {
        return builder
                .given("a good state")
                .uponReceiving("a query test interaction")
                .path("/")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body("{\"name\": \"harry\"}")
                .toPact();
    }

    @Pact(provider = OTHER_HTTP_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public RequestResponsePact otherHttpPact(PactDslWithProvider builder) {
        return builder
                .given("another good state")
                .uponReceiving("another query test interaction")
                .path("/other")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body("{\"name\": \"john\"}")
                .toPact();
    }

    @Pact(provider = MESSAGE_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public MessagePact messagePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("testParam1", "value1");

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        return builder.given("SomeProviderState")
                      .expectsToReceive("a test message")
                      .withMetadata(metadata)
                      .withContent(body)
                      .toPact();
    }

    @Pact(provider = OTHER_MESSAGE_PROVIDER_NAME, consumer = PACT_VERIFICATIONS_CONSUMER_NAME)
    public MessagePact otherMessagePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody();
        body.stringValue("testParamA", "valueA");

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("contentType", "application/json");

        return builder.given("SomeOtherProviderState")
                      .expectsToReceive("another test message")
                      .withMetadata(metadata)
                      .withContent(body)
                      .toPact();
    }

    @Test
    @PactVerifications({@PactVerification(HTTP_PROVIDER_NAME), @PactVerification(MESSAGE_PROVIDER_NAME)})
    public void shouldTestHttpAndMessagePacts() throws Exception {
        byte[] message = messageProvider.getMessage();
        assertNotNull(message);
        assertThat(new String(message), equalTo("{\"testParam1\":\"value1\"}"));

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("name", "harry");
        assertEquals(new ConsumerClient(httpProvider.getUrl()).getAsMap("/", ""), expectedResponse);
    }

    @Test
    @PactVerifications({@PactVerification(OTHER_HTTP_PROVIDER_NAME), @PactVerification(OTHER_MESSAGE_PROVIDER_NAME)})
    public void shouldTestOtherHttpAndOtherMessagePacts() throws Exception {
        byte[] message = otherMessageProvider.getMessage();
        assertNotNull(message);
        assertThat(new String(message), equalTo("{\"testParamA\":\"valueA\"}"));

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("name", "john");
        assertEquals(new ConsumerClient(otherHttpProvider.getUrl()).getAsMap("/other", ""), expectedResponse);
    }

    @Test
    @PactVerifications({@PactVerification(HTTP_PROVIDER_NAME), @PactVerification(OTHER_HTTP_PROVIDER_NAME)})
    public void shouldTestAllHttpPacts() throws Exception {
        assertEquals(new ConsumerClient(httpProvider.getUrl()).getAsMap("/", ""),
                     singletonMap ("name", "harry"));

        assertEquals(new ConsumerClient(otherHttpProvider.getUrl()).getAsMap("/other", ""),
                     singletonMap("name", "john"));
    }
}
