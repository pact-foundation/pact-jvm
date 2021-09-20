package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Rule
import org.junit.Test

/**
 * It is just an example how to build multipart request with multiple parts
 * Actual bodies of multipart requests are not compared
 */
class ExampleMultipartSpec {

  @Rule
  @SuppressWarnings('PublicInstanceField')
  public final PactProviderRule mockProvider = new PactProviderRule('File Service', this)

  @Pact(provider = 'File Service', consumer= 'Junit Consumer')
  RequestResponsePact createPact(PactDslWithProvider builder) {
    def multipartEntityBuilder = MultipartEntityBuilder.create()
      .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
      .addBinaryBody('file', '1,2,3,4\n5,6,7,8'.bytes, ContentType.create('text/csv'), 'data.csv')
      .addTextBody('textPart', 'sample text')
    builder
      .uponReceiving('a multipart file POST')
      .path('/upload')
      .method('POST')
      .body(multipartEntityBuilder)
      .willRespondWith()
      .status(201)
      .body('file uploaded ok', 'text/plain')
      .toPact()
  }

  @Test
  @PactVerification
  void runTest() {
    CloseableHttpClient httpclient = HttpClients.createDefault()
    httpclient.withCloseable {
      def data = MultipartEntityBuilder.create()
        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        .addBinaryBody('file', '1,2,3,4\n5,6,7,8'.bytes, ContentType.create('text/csv'), 'data.csv')
        .addTextBody('textPart', 'sample text')
        .build()
      def request = RequestBuilder
        .post(mockProvider.url + '/upload')
        .setEntity(data)
        .build()
      println('Executing request ' + request.requestLine)
      httpclient.execute(request)
    }
  }
}
