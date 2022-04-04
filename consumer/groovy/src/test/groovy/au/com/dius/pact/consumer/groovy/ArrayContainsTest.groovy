package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonSlurper
import org.junit.Test

@SuppressWarnings('ClosureAsLastMethodParameter')
class ArrayContainsTest {
  @Test
  void 'array contains matcher example'() {
    def service = new PactBuilder(PactSpecVersion.V4)
    service {
      serviceConsumer 'Order Processor'
      hasPactWith 'Siren Order Service'

      uponReceiving('get all orders')
      withAttributes(path: '/orders')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'application/vnd.siren+json']
      )
      withBody(mimeType: 'application/vnd.siren+json') {
        'class'(['entity'])
        entities eachLike {
          'class'(['entity'])
          rel = ['item']
          'properties' {
            id integer(1234)
          }
          links = [
            {
              rel = ['self']
              href url('http://localhost:9000', 'orders', regexp('\\d+', '1234'))
            }
          ]
          actions arrayContaining([
            {
              name 'update'
              method 'PUT'
              href url('http://localhost:9000', 'orders', regexp('\\d+', '1234'))
            },
            {
              name 'delete'
              method 'DELETE'
              href url('http://localhost:9000', 'orders', regexp('\\d+', '1234'))
            }
          ])
        }
        links = [
          {
            rel = ['self']
            href url('http://localhost:9000', 'orders')
          }
        ]
      }
    }

    assert service.runTest { server ->
      def http = new SimpleHttp(server.url)
      def response = http.get('/orders')
      assert response.statusCode == 200
      assert response.hasBody
      def result = new JsonSlurper().parse(response.reader)
      assert result.entities instanceof List
    } instanceof PactVerificationResult.Ok
  }
}
