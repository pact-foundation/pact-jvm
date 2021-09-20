package au.com.dius.pact.provider.junit5

import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.github.javafaker.Faker
import com.github.tomakehurst.wiremock.WireMockServer
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import ru.lanwen.wiremock.ext.WiremockResolver
import ru.lanwen.wiremock.ext.WiremockUriResolver

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

@Provider('Animal Profile Service V3')
@PactFolder('pacts')
@ExtendWith([
  WiremockResolver,
  WiremockUriResolver
])
@Slf4j
class EachLikeFromPactJsTest {
  private WireMockServer server

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider)
  void testTemplate(PactVerificationContext context) {
    context.verifyInteraction()
  }

  @State('is authenticated')
  @SuppressWarnings('EmptyMethod')
  void setAuthenticated() { }

  @State('Has an animal with ID')
  void hasAnimal(Map params) {
    def animal = params.id as String
    Faker faker = new Faker()

    def interests = []
    RandomUtils.nextInt(1, 5).times {
      interests << faker.backToTheFuture().quote()
    }

    def identifiers = [:]
    RandomUtils.nextInt(2, 5).times {
      identifiers[it.toString()] = [
        description: faker.book().title(),
        id: RandomUtils.nextInt(1, 999).toString()
      ]
    }

     server.stubFor(
      get(urlPathEqualTo('/animals/' + animal))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader('Content-Type', 'application/json;charset=utf-8')
          .withBody(JsonOutput.toJson([
            id: Integer.parseInt(animal),
            age: RandomUtils.nextInt(10, 50),
            animal: faker.animal().name(),
            available_from: DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(ZonedDateTime.now()),
            first_name: faker.name().firstName(),
            last_name: faker.name().lastName(),
            eligibility: [
              available: RandomUtils.nextBoolean(),
              previously_married: RandomUtils.nextBoolean()
            ],
            gender: RandomUtils.nextBoolean() ? 'M' : 'F',
            location: [
              country: faker.country().name(),
              description: faker.lebowski().quote(),
              post_code: RandomUtils.nextInt(1000, 9999)
            ],
            interests: interests,
            identifiers: identifiers
          ])))
    )
  }

  @State('Has no animals')
  @SuppressWarnings('EmptyMethod')
  void noAnimals() { }

  @BeforeEach
  void before(
    PactVerificationContext context,
    @WiremockResolver.Wiremock WireMockServer server,
    @WiremockUriResolver.WiremockUri String uri
  ) throws MalformedURLException {
    context.setTarget(HttpTestTarget.fromUrl(new URL(uri)))
    this.server = server
  }
}
