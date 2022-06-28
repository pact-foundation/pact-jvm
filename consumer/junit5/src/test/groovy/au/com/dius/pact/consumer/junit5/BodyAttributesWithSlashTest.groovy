package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.LambdaDsl
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ClassicHttpResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderWithSlashes', pactVersion = PactSpecVersion.V4)
class BodyAttributesWithSlashTest {
  @Pact(consumer = 'Consumer')
  V4Pact pact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request for some shipping info')
        .path('/shipping/v1')
      .willRespondWith()
        .status(200)
        // '{ "data": [ { "relationships": { "user/shippingAddress": { "data": { "id": "123", "type": "user/shipping-address" } } } } ] }'
        .body(LambdaDsl.newJsonBody( body -> {
          body.eachLike('data', o -> {
            o.object('relationships', r -> {
              r.object('user/shippingAddress', addr -> {
                addr.object('data', d -> {
                  d.stringMatcher('id', '\\d+', '123456')
                  d.stringType('type', 'user/shipping-address')
                })
              })
            })
          })
        }).build())
      .toPact(V4Pact)
  }

  @Test
  void testShippingInfo(MockServer mockServer) {
    ClassicHttpResponse httpResponse = Request.get("${mockServer.url}/shipping/v1")
      .execute().returnResponse() as ClassicHttpResponse
    assert httpResponse.code == 200
    assert httpResponse.entity.content.text == '{"data":[{"relationships":{"user/shippingAddress":{"data":{"id":"123456","type":"user/shipping-address"}}}}]}'
  }
}
