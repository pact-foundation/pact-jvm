package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClients
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderThatAcceptsGZippedBodies')
class GZippedBodyTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request with a zipped body')
        .method('POST')
        .path('/values')
        .body(new PactDslJsonBody().integerType('id'))
      .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody().integerType('id'))
      .toPact()
  }

  @Test
  void testFiles(MockServer mockServer) {
    def entity = EntityBuilder.create()
      .setText('{"id": 1}')
      .setContentType(ContentType.APPLICATION_JSON)
      .gzipCompress()
      .build()
    HttpClient httpClient = HttpClients.createDefault()
    def post = new HttpPost("${mockServer.url}/values")
    post.setEntity(entity)
    post.setHeader('Content-Type', ContentType.APPLICATION_JSON.toString())
    post.setHeader('Content-Encoding', 'gzip')
    post.setHeader('Accept-Encoding', 'gzip, deflate')
    def response = httpClient.execute(post)
    assert response.statusLine.statusCode == 200
  }
}
