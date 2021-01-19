package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.LambdaDsl
import au.com.dius.pact.consumer.dsl.PM
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'Siren Order Provider')
class HyperMediaPactTest {

  @Canonical
  class DeleteFirstOrderClient {
    String url

    @SuppressWarnings('UnnecessaryIfStatement')
    boolean execute() {
      HttpResponse httpResponse = Request.Get(url).execute().returnResponse()
      if (httpResponse.statusLine.statusCode == 200) {
        def root = httpResponse.entity.content.withCloseable { new JsonSlurper().parse(it) }
        def ordersUrl = root['links'].find { it['rel'] == ['orders'] }['href']
        httpResponse = Request.Get(ordersUrl).execute().returnResponse()
        if (httpResponse.statusLine.statusCode == 200) {
          def orders = httpResponse.entity.content.withCloseable { new JsonSlurper().parse(it) }
          def deleteAction = orders['entities'][0]['actions'].find { it['name'] == 'delete' }
          httpResponse = Request.Delete(deleteAction['href']).execute().returnResponse()
          httpResponse.statusLine.statusCode == 204
        } else {
          false
        }
      } else {
        false
      }
    }
  }

  @Pact(consumer = 'Siren Order Service')
  RequestResponsePact deleteFirstOrderTest(PactDslWithProvider builder) {
    builder
      // Get Root Request
      .uponReceiving('get root')
        .path('/')
      .willRespondWith()
        .status(200)
        .headers(['Content-Type': 'application/vnd.siren+json'])
        .body( LambdaDsl.newJsonBody { body ->
          body.array('class') {
            it.stringValue('representation')
          }
          body.array('links') { links ->
            links.object { link ->
              link.array('rel') {
                it.stringValue('orders')
              }
              link.matchUrl2('href', 'orders')
            }
          }
        }.build())

      // Get Orders Request
      .uponReceiving('get all orders')
        .path('/orders')
      .willRespondWith()
      .status(200)
      .headers(['Content-Type': 'application/vnd.siren+json'])
      .body( LambdaDsl.newJsonBody { body ->
        body.array('class') {
          it.stringValue('entity')
        }
        body.eachLike('entities') { entity ->
          entity.array('class') {
            it.stringValue('entity')
          }
          entity.array('rel') {
            it.stringValue('item')
          }
          entity.object('properties') {
            it.integerType('id', 1234)
          }
          entity.eachLike('links') { link ->
            link.array('rel') {
              it.stringValue('self')
            }
            link.matchUrl2('href', 'orders', PM.stringMatcher('\\d+', '1234'))
          }
          entity.arrayContaining('actions') { actions ->
            actions.object {
              it.stringValue('name', 'update')
              it.stringValue('method', 'PUT')
              it.matchUrl2('href', 'orders', PM.stringMatcher('\\d+', '1234'))
            }
            actions.object {
              it.stringValue('name', 'delete')
              it.stringValue('method', 'DELETE')
              it.matchUrl2('href', 'orders', PM.stringMatcher('\\d+', '1234'))
            }
          }
        }
        body.eachLike('links') { link ->
          link.array('rel') {
            it.stringValue('self')
          }
          link.matchUrl2('href', 'orders')
        }
      }.build())

      // Delete Order Request
      .uponReceiving('delete order')
        .method('DELETE')
        .matchPath('/orders/\\d+', '/orders/1234')
      .willRespondWith()
        .status(204)

      .toPact()
  }

  @Test
  void testDeleteOrder(MockServer mockServer) {
    DeleteFirstOrderClient client = new DeleteFirstOrderClient(mockServer.url)
    assert client.execute()
  }
}
