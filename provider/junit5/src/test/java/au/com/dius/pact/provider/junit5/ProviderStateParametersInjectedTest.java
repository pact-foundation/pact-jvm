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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Provider("ProviderStateParametersInjected")
@PactFolder("pacts")
@ExtendWith({
  WiremockResolver.class,
  WiremockUriResolver.class
})
public class ProviderStateParametersInjectedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderStateParametersInjectedTest.class);

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context,
                @WiremockResolver.Wiremock WireMockServer server,
                @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
      context.setTarget(HttpTestTarget.fromUrl(new URL(uri)));

      server.stubFor(
        get(urlPathEqualTo("/api/hello/John"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("content-type", "application/json")
            .withBody("{\"name\": \"John\"}")
          )
      );
    }

    @State("User exists")
    public Map<String, Object> defaultState(Map<String, Object> params) {
      LOGGER.debug("Provider state params = " + params);
      return Collections.emptyMap();
    }
}
