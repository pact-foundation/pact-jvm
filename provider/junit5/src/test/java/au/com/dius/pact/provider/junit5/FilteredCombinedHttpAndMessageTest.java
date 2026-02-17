package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.filter.InteractionFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Provider("test_provider_combined")
@PactFolder("pacts")
@ExtendWith({
  WiremockResolver.class,
  WiremockUriResolver.class
})
@PactFilter(value = "Synchronous/HTTP", filter = InteractionFilter.ByInteractionType.class)
public class FilteredCombinedHttpAndMessageTest {
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testTemplate(HttpRequest request, PactVerificationContext context) {
      if (request != null) {
        request.addHeader("X-ContractTest", "true");
      }

      context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context,
                @WiremockResolver.Wiremock WireMockServer server,
                @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
      context.setTarget(HttpTestTarget.fromUrl(new URL(uri)));

      server.stubFor(
        get(urlPathEqualTo("/data"))
          .withHeader("X-ContractTest", equalTo("true"))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("content-type", "application/json")
            .withBody("{}")
          )
      );
    }
}
