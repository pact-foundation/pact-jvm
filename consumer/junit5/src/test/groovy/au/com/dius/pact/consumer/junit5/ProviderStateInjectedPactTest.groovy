package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonOutput
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderStateService')
@SuppressWarnings(['JUnitPublicNonTestMethod', 'GStringExpressionWithinString'])
class ProviderStateInjectedPactTest {
  @Pact(provider = 'ProviderStateService', consumer = 'V3Consumer')
  RequestResponsePact articles(PactDslWithProvider builder) {
    def pact = builder
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

    def generators = pact.interactions.first().response.generators.toMap(PactSpecVersion.V3)
    assert generators == [
      body: ['$.userId': [type: 'ProviderState', expression: 'userId', dataType: 'INTEGER']],
      header: [LOCATION: [type: 'ProviderState', expression: 'http://server/users/${userId}', dataType: 'STRING']]
    ]

    pact
  }

  @Test
  void testArticles(MockServer mockServer) {
    HttpResponse httpResponse = Request.Post("${mockServer.url}/values")
      .bodyString(JsonOutput.toJson([userName: 'Test', userClass: 'Shoddy']), ContentType.APPLICATION_JSON)
      .execute().returnResponse()
    assert httpResponse.statusLine.statusCode == 200
    assert httpResponse.entity.content.text == '{"userName":"Test","userId":100}'
  }
}
