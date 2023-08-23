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

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Provider("ProviderStateService")
@PactFolder("pacts")
@ExtendWith({
  WiremockResolver.class,
  WiremockUriResolver.class
})
public class ProviderStateInjectedTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderStateInjectedTest.class);

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(PactVerificationContext context) {
      context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context,
                @WiremockResolver.Wiremock WireMockServer server,
                @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
      LOGGER.info("BeforeEach - " + uri);

      context.setTarget(HttpTestTarget.fromUrl(new URL(uri)));

      server.stubFor(
        post(urlPathEqualTo("/values"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Location", "http://server/users/666554433")
            .withHeader("content-type", "application/json")
            .withBody("{\"userId\": 666554433,\"userName\": \"Test\"}")
          )
      );
    }

    @State("a provider state with injectable values")
    public Map<String, Object> defaultState(Map<String, Object> params) {
      LOGGER.info("Default state: " + params);
      LOGGER.info("valueB: " + params.get("valueB") + " {" + params.get("valueB").getClass() + "}");
      assertThat(params.get("valueB"), is(equalTo(BigInteger.valueOf(100))));

      HashMap<String, Object> map = new HashMap<>();
      map.put("userId", 666554433);
      return map;
    }
}
