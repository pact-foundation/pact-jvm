package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
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
      .setMode(HttpMultipartMode.EXTENDED)
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
    def httpclient = HttpClients.createDefault()
    httpclient.withCloseable { client ->
      def data = MultipartEntityBuilder.create()
        .setMode(HttpMultipartMode.EXTENDED)
        .addBinaryBody('file', '1,2,3,4\n5,6,7,8'.bytes, ContentType.create('text/csv'), 'data.csv')
        .addTextBody('textPart', 'sample text')
        .build()
      def request = new HttpPost(mockProvider.url + '/upload')
      request.setEntity(data)
      println('Executing request ' + request)
      client.execute(request)
    }
  }
}
