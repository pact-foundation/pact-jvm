package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.SimpleHttp
import org.junit.Test

class V4MatchBooleanTest {

  @Test
  void "matching boolean values in query parameter"() {

    def service = new PactBuilder(PactSpecVersion.V4)
    service {
      serviceConsumer 'BooleanConsumer'
      hasPactWith 'BooleanService'

      uponReceiving('a request with boolean values')
      withAttributes(method: 'get', path: '/test', query: [name: bool(true), status: bool(false)])
      willRespondWith(status: 200)
    }

    PactVerificationResult result = service.runTest { server ->
      def client = new SimpleHttp(server.url)
      def response = client.get('/test', [status: 'good', name: 'true'])

      assert response.statusCode == 500
    }
    assert result instanceof PactVerificationResult.Mismatches
    assert result.mismatches*.mismatches.flatten { it.mismatch } == ['Expected \'good\' (String) to match a boolean']
  }

  @Test
  void "matching boolean values in headers"() {

    def service = new PactBuilder(PactSpecVersion.V4)
    service {
      serviceConsumer 'BooleanConsumer'
      hasPactWith 'BooleanService'

      uponReceiving('a request with boolean values in header')
      withAttributes(method: 'get', path: '/test', headers: [test: bool(true), test2: bool(true)])
      willRespondWith(status: 200)
    }

    PactVerificationResult result = service.runTest { server ->
      def client = new SimpleHttp(server.url)
      def response = client.get('/test', [:], [test: 'yes', test2: 'false'])

      assert response.statusCode == 500
    }
    assert result instanceof PactVerificationResult.Mismatches
    assert result.mismatches*.mismatches.flatten { it.mismatch } == ['Expected \'yes\' (String) to match a boolean']
  }
}
