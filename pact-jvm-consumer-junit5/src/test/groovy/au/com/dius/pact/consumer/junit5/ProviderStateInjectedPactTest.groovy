package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.Pact
import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderStateService')
@SuppressWarnings(['JUnitPublicNonTestMethod', 'GStringExpressionWithinString'])
class ProviderStateInjectedPactTest {
  @Pact(provider = 'ProviderStateService', consumer = 'V3Consumer')
  RequestResponsePact articles(PactDslWithProvider builder) {
    builder
      .given('a provider state with injectable values', [valueA: 'A', valueB: 100])
      .uponReceiving('a request')
        .path('/values')
        .method('POST')
      .willRespondWith()
        .headerFromProviderState('LOCATION', 'http://server/users/${userId}', 'http://server/users/666')
        .status(200)
      .body(
        new PactDslJsonBody()
          .stringValue('userName', 'Test')
          .valueFromProviderState('userId', 'userId', 100)
      )
      .toPact()
  }

  @Test
  void testArticles(MockServer mockServer) {
    HttpResponse httpResponse = Request.Post("${mockServer.url}/values")
      .bodyString(JsonOutput.toJson([userName: 'Test', userClass: 'Shoddy']), ContentType.APPLICATION_JSON)
      .execute().returnResponse()
    assert httpResponse.statusLine.statusCode == 200
    assert httpResponse.entity.content.text == '{"userName":"Test","userId":100}'
  }

  @AfterAll
  static void checkPactFile() {
    def pactFile = new File("${PactConsumerConfig.pactRootDir()}/V3Consumer-ProviderStateService.json")
    def json = new JsonSlurper().parse(pactFile)
    assert json.metadata.pactSpecification.version == '3.0.0'
    def generators = json.interactions.first().response.generators
    assert generators == [
      body: ['$.userId': [type: 'ProviderState', expression: 'userId']],
      header: [LOCATION: [type: 'ProviderState', expression: 'http://server/users/${userId}']]
    ]
  }
}
