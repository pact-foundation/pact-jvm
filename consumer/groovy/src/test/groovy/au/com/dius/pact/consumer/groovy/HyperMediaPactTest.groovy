package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import groovyx.net.http.HttpBuilder
import org.junit.Test

class HyperMediaPactTest {

  @Canonical
  class DeleteFirstOrderClient {
    String url

    @SuppressWarnings('UnnecessaryIfStatement')
    boolean execute() {
      def http = HttpBuilder.configure {
        request.uri = url
        response.parser('application/vnd.siren+json') { config, resp ->
          new JsonSlurper().parse(resp.reader)
        }
      }
      def root = http.get()
      def ordersUrl = root['links'].find { it['rel'] == ['orders'] }['href']
      def orders = http.get {
        request.uri = ordersUrl
      }
      def deleteAction = orders['entities'][0]['actions'].find { it['name'] == 'delete' }
      http.delete {
        request.uri = deleteAction['href']
        response.when(204) { true }
      }
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
