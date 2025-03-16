package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.core.model.v4.MessageContents;
import au.com.dius.pact.core.support.json.JsonParser;
import au.com.dius.pact.provider.MessageAndMetadata;
import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Provider("SyncMessageProviderStateService")
@PactFolder("pacts")
public class SyncMessageWithProviderStateTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncMessageWithProviderStateTest.class);

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @State("the provider injects a 'stateValue'")
    public Map<String, Object> defaultState() {
      return Map.of(
              "stateValue", "PROVIDER_STATE_VALUE"
      );
    }

    @PactVerifyProvider("State has been inserted in request message")
    public MessageAndMetadata stateHasBeenInserted(MessageContents messageContents) {
        var json = JsonParser.parseString(messageContents.getContents().valueAsString());
        var value = json.asObject().get("state").asString();

        // This is what this test is truly asserting
        assertThat(value, is("PROVIDER_STATE_VALUE"));

        return new MessageAndMetadata(
                "{\"state\": \"PROVIDER_STATE_VALUE\"}".getBytes(), Map.of());
    }
}
