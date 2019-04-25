package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactConsumerConfig
import au.com.dius.pact.consumer.PactVerificationResult
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
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
      withAttributes(method: 'POST', path: '/values')
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
      def client = new RESTClient(mockServer.url, 'application/json')
      def response = client.post(path: '/values', body: [userName: 'Test', userClass: 'Shoddy'])

      assert response.status == 200
      assert response.data == [userName: 'Test', userId: 100]
    }
    assert result instanceof PactVerificationResult.Ok

    def pactFile = new File("${PactConsumerConfig.pactDirectory}/V3Consumer-ProviderStateService.json")
    def json = new JsonSlurper().parse(pactFile)
    assert json.metadata.pactSpecification.version == '3.0.0'
    def generators = json.interactions.first().response.generators
    assert generators == [
      body: ['$.userId': [type: 'ProviderState', expression: 'userId']],
      header: [LOCATION: [type: 'ProviderState', expression: 'http://server/users/${userId}']]
    ]
  }
}
