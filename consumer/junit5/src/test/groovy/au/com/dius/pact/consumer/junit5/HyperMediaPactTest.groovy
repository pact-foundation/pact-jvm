package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.LambdaDsl
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
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
      /*
  const ordersResource = await client.follow('orders')
  const ordersResp = await ordersResource.get()

  const firstOrder = ordersResp.getEmbedded()[0]
  let deleteAction = firstOrder.action('delete')
  deleteAction.client = client
  await deleteAction.submit()
       */

      HttpResponse httpResponse = Request.Get(url).execute().returnResponse()
      if (httpResponse.statusLine.statusCode == 200) {
        true
      } else {
        false
      }
    }
  }

  /*
  it('deletes the first order using the delete action', () => {
    provider

      // Get Root Request
      .uponReceiving("get root")
      .withRequest({
        path: "/",
      })
      .willRespondWith({
        status: 200,
        headers: {
          'Content-Type': 'application/vnd.siren+json'
        },
        body: {
          class: [ "representation"],
          links: [{"rel":["orders"], "href":  url(["orders"]) }]
        }
      })

      // Get Orders Request
      .uponReceiving("get all orders")
      .withRequest({
        path: "/orders",
      })
      .willRespondWith({
        status: 200,
        headers: {
          'Content-Type': 'application/vnd.siren+json'
        },
        body: {
          class: [ "entity" ],
          entities: eachLike({
            class: [ "entity" ],
            rel: [ "item" ],
            properties: {
              "id": integer(1234)
            },
            links: [
              {
                "rel": [ "self" ],
                "href": url(["orders", regex("\\d+", "1234")])
              }
            ],
            "actions": arrayContaining(
              {
                "name": "update",
                "method": "PUT",
                "href": url(["orders", regex("\\d+", "1234")])
              },
              {
                "name": "delete",
                "method": "DELETE",
                "href": url(["orders", regex("\\d+", "1234")])
              }
            )
          }),
          links: [
            {
              rel: [ "self" ],
              href: url(["orders"])
            }
          ]
        }
      })

      // Delete Order Request
      .uponReceiving("delete order")
      .withRequest({
        method: "DELETE",
        path: regex("/orders/\\d+", "/orders/1234"),
      })
      .willRespondWith({
        status: 200
      })

    return provider.executeTest(mockserver => {
      return expect(deleteFirstOrder(mockserver.url)).to.eventually.be.true
    })
  })
})
   */

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
      .toPact()
  }

  @Test
  void testDeleteOrder(MockServer mockServer) {
    DeleteFirstOrderClient client = new DeleteFirstOrderClient(mockServer.url)
    assert client.execute()
  }
}
