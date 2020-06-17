package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class BinaryFileSpec extends Specification {

  def 'handles bodies from form posts'() {
    given:
    def pdf = BinaryFileSpec.getResourceAsStream('/sample.pdf').bytes
    def service = new PactBuilder()
    service {
      serviceConsumer 'Consumer'
      hasPactWith 'File Service'
      uponReceiving('a multipart file POST')
      withAttributes(path: '/get-doco')
      willRespondWith(status: 200, body: pdf, headers: ['Content-Type': 'application/pdf'])
    }

    when:
    def result = service.runTest { MockServer mockServer, context ->
      CloseableHttpClient httpclient = HttpClients.createDefault()
      def response = httpclient.withCloseable {
        def request = RequestBuilder.get(mockServer.url + '/get-doco').build()
        httpclient.execute(request)
      }
      assert response.statusLine.statusCode == 200
      assert response.entity.contentLength == pdf.size()
    }

    then:
    result instanceof PactVerificationResult.Ok
  }
}
