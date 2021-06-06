package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import groovy.json.JsonSlurper
import groovyx.net.http.ContentTypes
import groovyx.net.http.HttpBuilder
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
      def http = HttpBuilder.configure { request.uri = server.url }
      http.get {
        request.uri.path = '/orders'
        response.parser(ContentTypes.ANY[0]) { config, resp ->
          assert resp.statusCode == 200
          assert resp.hasBody
          def result = new JsonSlurper().parse(resp.reader)
          assert result.entities instanceof List
        }
      }
    } instanceof PactVerificationResult.Ok
  }
}
