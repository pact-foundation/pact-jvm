package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;

@Provider("myAwesomeService")
@PactFolder("pacts")
@ExtendWith({
  WiremockResolver.class,
  WiremockUriResolver.class
})
public class HttpsContractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpsContractTest.class);

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context,
                @WiremockResolver.Wiremock(factory = WiremockHttpsConfigFactory.class) WireMockServer server) {
        // Rest data
        // Mock dependent service responses
        // ...
      LOGGER.info("BeforeEach - " + server.httpsPort());

      context.setTarget(new HttpsTestTarget("localhost", server.httpsPort(), "/", true));

      server.stubFor(
        get(urlPathEqualTo("/data"))
          .willReturn(aResponse()
            .withStatus(204)
            .withHeader("Location", format("http://localhost:%s/ticket/%s", server.port(), "1234")
            )
            .withHeader("X-Ticket-ID", "1234"))
      );
    }

    @State("default")
    public void toDefaultState() {
        // Prepare service before interaction that require "default" state
        // ...
      LOGGER.info("Now service in default state");
    }

    @State("state 2")
    public void toSecondState(Map params) {
        // Prepare service before interaction that require "state 2" state
        // ...
        LOGGER.info("Now service in 'state 2' state: " + params);
    }
}
