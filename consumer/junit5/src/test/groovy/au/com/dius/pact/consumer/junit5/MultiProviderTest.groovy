package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody

@SuppressWarnings('UnusedMethodParameter')
@ExtendWith(PactConsumerTestExt)
@Disabled
class MultiProviderTest {

  @Pact(provider = 'provider1', consumer= 'consumer')
  RequestResponsePact pact1(PactDslWithProvider builder) {
    builder
      .uponReceiving('a new user')
        .path('/some-service/users')
        .method('POST')
        .body(newJsonBody {
          it.stringValue('name', 'bob')
        }.build())
      .willRespondWith()
        .status(201)
        .matchHeader('Location', 'http(s)?://\\w+:\\d+//some-service/user/\\w{36}$')
      .toPact()
  }

  @Pact(provider = 'provider2', consumer= 'consumer')
  RequestResponsePact pact2(PactDslWithProvider builder) {
    builder
      .uponReceiving('a new user')
      .path('/some-service/users')
      .method('POST')
      .body(newJsonBody {
        it.numberType('id')
      }.build())
      .willRespondWith()
      .status(204)
      .toPact()
  }

  @Test
  @PactTestFor(pactMethods = ['pact1', 'pact2'])
  void runTest1(MockServer mockServer1, MockServer mockServer2) {
    def http = HttpBuilder.configure { request.uri = mockServer1.url }

    http.post {
      request.uri.path = '/some-service/users'
      request.body = user()
      request.contentType = 'application/json'

      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 201
        assert fs.headers.find { it.key == 'Location' }?.value?.contains(SOME_SERVICE_USER)
      }
    }

    http.get {
      request.uri.path = SOME_SERVICE_USER + EXPECTED_USER_ID
      request.contentType = 'application/json'
      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 200
      }
    }

  }
}
