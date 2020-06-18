package au.com.dius.pact.consumer.junit

import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Rule
import org.junit.Test

class BinaryFileSpec {

  @Rule
  @SuppressWarnings('PublicInstanceField')
  public final PactProviderRule mockProvider = new PactProviderRule('File Service', this)

  @Pact(provider = 'File Service', consumer= 'PDF Consumer')
  RequestResponsePact createPact(PactDslWithProvider builder) {
    def pdf = BinaryFileSpec.getResourceAsStream('/sample.pdf').bytes
    builder
      .uponReceiving('a request for a PDF')
      .path('/get-file')
      .method('GET')
      .willRespondWith()
      .status(200)
      .withBinaryData(pdf, 'application/pdf')
      .toPact()
  }

  @Test
  @PactVerification
  void runTest() {
    CloseableHttpClient httpclient = HttpClients.createDefault()
    httpclient.withCloseable {
      def request = RequestBuilder
        .get(mockProvider.url + '/get-file')
        .build()
      def response = httpclient.execute(request)
      assert response.statusLine.statusCode == 200
      assert response.entity.contentType.value == 'application/pdf'
      assert response.entity.content.bytes[0..7] == [37, 80, 68, 70, 45, 49, 46, 53] as byte[]
    }
  }
}
