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
    builder
      .uponReceiving('a request for a PDF')
      .path('/get-file')
      .method('GET')
      .willRespondWith()
      .status(200)
      .body('0111010001110111', 'application/pdf')
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
      assert new String(response.entity.content.bytes) == '0111010001110111'
    }
  }
}
