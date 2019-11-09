package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock;
import ru.lanwen.wiremock.ext.WiremockUriResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri;

import java.net.MalformedURLException;
import java.net.URL;

@Provider("providerWithMultipleInteractions")
@PactFolder("pacts")
@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class StateAnnotationsOnAdditionalClassTest {

  private WireMockServer server;

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @BeforeEach
  void before(PactVerificationContext context, @Wiremock WireMockServer server,
      @WiremockUri String uri) throws MalformedURLException {
    this.server = server;
    context.addStateChangeHandlers(new StateClass1(server), new StateClass2(server));
    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)));
  }

  public WireMockServer server() {
    return this.server;
  }
}
