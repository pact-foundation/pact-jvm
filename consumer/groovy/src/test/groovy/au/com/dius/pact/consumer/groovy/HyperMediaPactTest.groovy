package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.junit.Test

class HyperMediaPactTest {

  @Canonical
  class DeleteFirstOrderClient {
    String url

    @SuppressWarnings('UnnecessaryIfStatement')
    boolean execute() {
      def http = new SimpleHttp(url)
      def resp = http.get('/')
      def root = new JsonSlurper().parse(resp.inputStream)
      def ordersUrl = root['links'].find { it['rel'] == ['orders'] }['href']
      resp = http.get(ordersUrl)
      def orders = new JsonSlurper().parse(resp.inputStream)
      def deleteAction = orders['entities'][0]['actions'].find { it['name'] == 'delete' }
      def deleteResp = http.delete(deleteAction['href'])
      deleteResp.statusCode == 204
    }
  }

  @Test
  void testDeleteOrder() {
    def service = new PactBuilder()
    service {
      serviceConsumer 'Hypermedia Order Processor'
      hasPactWith 'Siren Order Service'

      uponReceiving('get root')
      withAttributes(path: '/')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'application/vnd.siren+json']
      )
      withBody(mimeType: 'application/vnd.siren+json') {
        'class'([ 'representation' ])
        links eachLike {
          rel(['orders' ])
          href url('', 'orders')
        }
      }

      uponReceiving('get all orders')
      withAttributes(path: '/orders')
      willRespondWith(
        status: 200,
        headers: ['Content-Type': 'application/vnd.siren+json']
      )
      withBody(mimeType: 'application/vnd.siren+json') {
        'class'([ 'entity' ])
        entities eachLike {
          'class'([ 'entity' ])
          rel([ 'item' ])
          properties {
            id integer(1234)
          }
          links([{
            rel(['self' ])
            href url('', 'orders', regexp(/\d+/, '1234'))
          }])
          actions arrayContaining([
            {
              name 'update'
              method 'PUT'
              href url('', 'orders', regexp('\\d+', '1234'))
            }, {
              name 'delete'
              method 'DELETE'
              href url('', 'orders', regexp('\\d+', '1234'))
            }
          ])
        }
        links([{
          rel(['self' ])
          href url('', 'orders')
        }])
      }

      uponReceiving('delete order')
      withAttributes(method: 'DELETE', path: regexp('/orders/\\d+', '/orders/1234'))
      willRespondWith(status: 204)
    }

    assert service.runTest(specificationVersion: PactSpecVersion.V3) { MockServer server ->
      DeleteFirstOrderClient client = new DeleteFirstOrderClient(server.url)
      assert client.execute()
    } instanceof PactVerificationResult.Ok
  }
}
