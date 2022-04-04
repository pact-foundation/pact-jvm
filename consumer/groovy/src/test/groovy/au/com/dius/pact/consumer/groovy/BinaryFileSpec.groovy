package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.support.SimpleHttp
import spock.lang.Specification

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
      def client = new SimpleHttp(mockServer.url)
      def response = client.get('/get-doco')
      assert response.statusCode == 200
      assert response.contentLength == pdf.size()
    }

    then:
    result instanceof PactVerificationResult.Ok
  }
}
