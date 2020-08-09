package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.support.BuiltToolConfig
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.junit.Test

@SuppressWarnings('GStringExpressionWithinString')
class ProviderStateInjectedPactTest {

  @Test
  void "example test with values from the provider state"() {

    def service = new PactBuilder()
    service {
      serviceConsumer 'V3Consumer'
      hasPactWith 'ProviderStateService'
    }

    service {
      given('a provider state with injectable values', [valueA: 'A', valueB: 100])
      uponReceiving('a request')
      withAttributes(method: 'POST', path: fromProviderState(
        '/shoppingCart/v2.0/shoppingCart/${shoppingcartId}',
        '/shoppingCart/v2.0/shoppingCart/ShoppingCart_05540051-1155-4557-8080-008a802200aa'))
      withBody {
        userName 'Test'
        userClass 'Shoddy'
      }
      willRespondWith(status: 200, headers:
        [LOCATION: fromProviderState('http://server/users/${userId}', 'http://server/users/666')])
      withBody {
        userName 'Test'
        userId fromProviderState('userId', 100)
      }
    }

    PactVerificationResult result = service.runTest { mockServer, context ->
      def client = HttpBuilder.configure {
        request.uri = mockServer.url
        request.contentType = 'application/json'
      }
      def resp = client.post(FromServer) {
        request.uri.path = '/shoppingCart/v2.0/shoppingCart/ShoppingCart_05540051-1155-4557-8080-008a802200aa'
        request.body = [userName: 'Test', userClass: 'Shoddy']
        response.parser(ContentTypes.ANY) { config, r ->
          r
        }
      }

      assert resp.statusCode == 200
      assert new JsonSlurper().parse(resp.inputStream) == [userName: 'Test', userId: 100]
    }
    assert result instanceof PactVerificationResult.Ok

    def pactFile = new File("${BuiltToolConfig.pactDirectory}/V3Consumer-ProviderStateService.json")
    def json = new JsonSlurper().parse(pactFile)
    assert json.metadata.pactSpecification.version == '3.0.0'
    def interaction = json.interactions.first()
    assert interaction.request.path ==
      '/shoppingCart/v2.0/shoppingCart/ShoppingCart_05540051-1155-4557-8080-008a802200aa'
    def generators = interaction.request.generators
    assert generators == [
      path: [type: 'ProviderState', expression: '/shoppingCart/v2.0/shoppingCart/${shoppingcartId}',
             'dataType': 'STRING']
    ]
    generators = interaction.response.generators
    assert generators == [
      body: ['$.userId': [type: 'ProviderState', expression: 'userId', 'dataType': 'INTEGER']],
      header: [LOCATION: [type: 'ProviderState', expression: 'http://server/users/${userId}', 'dataType': 'STRING']]
    ]
  }
}
