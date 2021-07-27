package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import groovy.json.JsonOutput
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody

@SuppressWarnings('UnusedMethodParameter')
@ExtendWith(PactConsumerTestExt)
class MultiProviderTest {

  @Pact(provider = 'provider1', consumer= 'consumer')
  RequestResponsePact pact1(PactDslWithProvider builder) {
    builder
      .uponReceiving('a new user request')
        .path('/users')
        .method('POST')
        .body(newJsonBody {
          it.stringType('name', 'bob')
        }.build())
      .willRespondWith()
        .status(201)
        .matchHeader('Location', 'http(s)?://\\w+:\\d{4}/user/\\d{16}')
      .toPact()
  }

  @Pact(provider = 'provider2', consumer= 'consumer')
  RequestResponsePact pact2(PactDslWithProvider builder) {
    builder
      .uponReceiving('a new user')
      .path('/users')
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
  void runTest(@ForProvider('provider1') MockServer mockServer1, @ForProvider('provider2') MockServer mockServer2) {
    def http = HttpBuilder.configure { request.uri = mockServer1.url }

    def id = http.post {
      request.uri.path = '/users'
      request.body = JsonOutput.toJson([name: 'Fred'])
      request.contentType = 'application/json'

      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 201

        def value = fs.headers.find { it.key == 'Location' }?.value
        assert value
        value.split('/').last() as BigInteger
      }
    }

    def http2 = HttpBuilder.configure { request.uri = mockServer2.url }
    http2.post {
      request.uri.path = '/users'
      request.contentType = 'application/json'
      request.body = JsonOutput.toJson([id: id])
      response.success { FromServer fs, Object body ->
        assert fs.statusCode == 204
      }
    }
  }
}
