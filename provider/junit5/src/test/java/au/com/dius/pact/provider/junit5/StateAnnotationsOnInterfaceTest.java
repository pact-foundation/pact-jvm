package au.com.dius.pact.provider.junit5;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockResolver.Wiremock;
import ru.lanwen.wiremock.ext.WiremockUriResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver.WiremockUri;

@Provider("providerWithMultipleInteractions")
@PactFolder("pacts")
@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
class StateAnnotationsOnInterfaceTest implements StateInterface1, StateInterface2 {

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
    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)));
  }

  @Override
  public WireMockServer server() {
    return this.server;
  }
}
