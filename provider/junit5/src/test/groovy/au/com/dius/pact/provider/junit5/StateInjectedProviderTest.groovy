package au.com.dius.pact.provider.junit5

import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.provider.junit.Provider
import au.com.dius.pact.provider.junit.State
import au.com.dius.pact.provider.junit.loader.PactFolder
import com.github.tomakehurst.wiremock.WireMockServer
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Provider('XmlInJsonService')
@PactFolder('pacts')
@ExtendWith([
  WiremockResolver,
  WiremockUriResolver
])
@Slf4j
class StateInjectedProviderTest {

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(Pact pact, Interaction interaction, HttpRequest request, PactVerificationContext context) {
    log.info("testTemplate called: ${pact.provider.name}, ${interaction.description}")
    request.addHeader('X-ContractTest', 'true')

    context.verifyInteraction()
  }

  @BeforeAll
  static void setUpService() {
    //Run DB, create schema
    //Run service
    //...
    log.info('BeforeAll - setUpService ')
  }

  @BeforeEach
  void before(PactVerificationContext context, @WiremockResolver.Wiremock WireMockServer server,
              @WiremockUriResolver.WiremockUri String uri) throws MalformedURLException {
    // Rest data
    // Mock dependent service responses
    // ...
    log.info("BeforeEach - $uri")

    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)))

    server.stubFor(
      post(urlPathEqualTo('/data'))
        .withHeader('X-ContractTest', equalTo('true'))
        .withRequestBody(matchingJsonPath('$.[?(@.entityName =~ /\\w+/)]'))
        .willReturn(aResponse()
          .withStatus(201)
          .withHeader('Location', "http://localhost:${server.port()}/entity/1234")
          .withHeader('Content-Type', 'application/json')
          .withBody('{"accountId": "4beb44f1-53f7-4281-abcd-12c06d682067"}'))
    )
  }

  @State('create XML entity')
  Map<String, Object> createXmlEntityState() {
    log.info('create XML entity state')
    [eName: RandomStringUtils.randomAlphanumeric(20)]
  }
}
