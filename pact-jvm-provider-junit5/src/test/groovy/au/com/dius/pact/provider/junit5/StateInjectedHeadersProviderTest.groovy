package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.loader.PactFolder
import com.github.tomakehurst.wiremock.WireMockServer
import groovy.util.logging.Slf4j
import org.apache.http.HttpRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Provider('providerInjectedHeaders')
@PactFolder('pacts')
@ExtendWith([
  WiremockResolver,
  WiremockUriResolver
])
@Slf4j
class StateInjectedHeadersProviderTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(Pact pact, Interaction interaction, HttpRequest request, PactVerificationContext context) {
    log.info("testTemplate called: ${pact.provider.name}, ${interaction.description}")
    request.addHeader('X-ContractTest', 'true')

    context.verifyInteraction()
  }

  @BeforeEach
  void before(PactVerificationContext context, @WiremockResolver.Wiremock WireMockServer server,
              @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
    log.info("BeforeEach - $uri")

    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)))

    server.stubFor(
      post(urlPathEqualTo('/accounts'))
        .withHeader('X-ContractTest', equalTo('true'))
        .willReturn(aResponse()
          .withStatus(201)
          .withHeader('Location', 'http://localhost:8090/accounts/1234'))
    )
  }

  @State('an active account exists')
  Map<String, Object> createAccount() {
    [
      port: 8090,
      accountId: '1234'
    ]
  }
}
