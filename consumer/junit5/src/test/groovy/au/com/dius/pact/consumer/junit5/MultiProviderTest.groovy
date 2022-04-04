package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonOutput
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
  @PactTestFor(pactMethods = ['pact1', 'pact2'], pactVersion = PactSpecVersion.V3)
  void runTest(@ForProvider('provider1') MockServer mockServer1, @ForProvider('provider2') MockServer mockServer2) {
    def http = new SimpleHttp(mockServer1.url)

    def response = http.post('/users', JsonOutput.toJson([name: 'Fred']),
      'application/json; charset=UTF-8')
    assert response.statusCode == 201
    def value = response.headers['Location'].first()
    assert value
    def id = value.split('/').last() as BigInteger

    def http2 = new SimpleHttp(mockServer2.url)
    def response2 = http2.post('/users', JsonOutput.toJson([id: id]),
      'application/json; charset=UTF-8')
    assert response2.statusCode == 204
  }
}
