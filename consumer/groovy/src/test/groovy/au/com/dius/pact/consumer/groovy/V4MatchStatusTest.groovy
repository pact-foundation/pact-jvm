package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.model.PactSpecVersion
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
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
      def client = HttpBuilder.configure {
        request.uri = server.url
      }
      def response = client.get(FromServer) {
        request.uri.path = '/test'
        response.success { FromServer fs, Object body -> fs }
        response.failure { FromServer fs, Object body -> fs }
      }

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
      def client = HttpBuilder.configure {
        request.uri = server.url
      }
      def response = client.get(FromServer) {
        request.uri.path = '/test'
        response.success { FromServer fs, Object body -> fs }
        response.failure { FromServer fs, Object body -> fs }
      }

      assert response.statusCode == 400
    }
    assert result instanceof PactVerificationResult.Ok
  }
}
