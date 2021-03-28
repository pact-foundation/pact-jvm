package au.com.dius.pact.provider.junit5

import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.github.tomakehurst.wiremock.WireMockServer
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Provider('ThriftJsonService')
@PactFolder('pacts')
@ExtendWith([
  WiremockResolver,
  WiremockUriResolver
])
@Slf4j
class ThriftStateInjectedProviderTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction()
  }

  @BeforeEach
  void before(PactVerificationContext context, @WiremockResolver.Wiremock WireMockServer server,
              @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)))
    System.setProperty('pact.content_type.override.application/x-thrift', 'json')

    server.stubFor(
      get(urlPathEqualTo('/data/111122223333'))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/x-thrift')
          .withBody('{"accountId": "abc123"}'))
    )
  }

  @State('default state')
  Map<String, Object> defaultState() {
    [
      id: '111122223333',
      accountId: 'abc123'
    ]
  }
}
