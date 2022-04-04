package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.SimpleHttp
import org.junit.Test

class V4MatchStatusTest {

  @Test
  void "matching status codes"() {

    def service = new PactBuilder(PactSpecVersion.V4)
    service {
      serviceConsumer 'V4Consumer'
      hasPactWith 'V4Service'

      uponReceiving('a test request')
      withAttributes(method: 'get', path: '/test')
      willRespondWith(status: successStatus())
    }

    PactVerificationResult result = service.runTest { server ->
      def client = new SimpleHttp(server.url)
      def response = client.get('/test')

      assert response.statusCode == 200
    }
    assert result instanceof PactVerificationResult.Ok
  }

  @Test
  void "matching error status codes"() {

    def service = new PactBuilder(PactSpecVersion.V4)
    service {
      serviceConsumer 'V4Consumer'
      hasPactWith 'V4Service'

      uponReceiving('a test request, part 2')
      withAttributes(method: 'get', path: '/test')
      willRespondWith(status: clientErrorStatus())
    }

    PactVerificationResult result = service.runTest { server ->
      def client = new SimpleHttp(server.url)
      def response = client.get('/test')

      assert response.statusCode == 400
    }
    assert result instanceof PactVerificationResult.Ok
  }
}
