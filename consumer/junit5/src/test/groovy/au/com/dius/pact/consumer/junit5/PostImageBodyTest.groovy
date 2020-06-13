package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderThatAcceptsImages')
class PostImageBodyTest {
  @Pact(consumer = 'Consumer')
  RequestResponsePact pact(PactDslWithProvider builder) {
    PostImageBodyTest.getResourceAsStream('/ron.jpg').withCloseable { stream ->
      builder
        .uponReceiving('a request with an image')
        .method('POST')
        .path('/images')
        .withFileUpload('photo', 'ron.jpg', 'image/jpeg', stream.bytes)
        .willRespondWith()
        .status(200)
        .body(new PactDslJsonBody()
          .integerType('version', 1)
          .integerType('status', 0)
          .stringValue('errorMessage', '')
          .array('issues').closeArray())
        .toPact()
    }
  }

  @Test
  void testFiles(MockServer mockServer) {
    CloseableHttpClient httpclient = HttpClients.createDefault()
    def result = httpclient.withCloseable {
      PostImageBodyTest.getResourceAsStream('/RAT.JPG').withCloseable { stream ->
        def data = MultipartEntityBuilder.create()
          .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
          .addBinaryBody('photo', stream, ContentType.create('image/jpeg'), 'ron.jpg')
          .build()
        def request = RequestBuilder
          .post(mockServer.url + '/images')
          .setEntity(data)
          .build()
        httpclient.execute(request)
      }
    }
    assert result.statusLine.statusCode == 200
  }
}
