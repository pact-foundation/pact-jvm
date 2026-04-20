package au.com.dius.pact.consumer.spock

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.support.SimpleHttp
import spock.lang.Specification

@SuppressWarnings(['JUnitPublicNonTestMethod', 'FactoryMethodName'])
@PactConsumerSpockTest
class PactConsumerSpockExtSpec extends Specification {

  MockServer mockServer

  @Pact(provider = 'spock_articles_provider', consumer = 'spock_consumer')
  RequestResponsePact articles(PactDslWithProvider builder) {
    builder
      .given('articles exist')
      .uponReceiving('a request for articles')
        .path('/articles')
        .method('GET')
      .willRespondWith()
        .status(200)
        .body('[{"id":1,"name":"Article 1"}]')
        .headers(['Content-Type': 'application/json'])
      .toPact()
  }

  @Pact(provider = 'spock_users_provider', consumer = 'spock_consumer')
  RequestResponsePact users(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request for users')
        .path('/users')
        .method('GET')
      .willRespondWith()
        .status(200)
        .body('[{"id":42}]')
        .headers(['Content-Type': 'application/json'])
      .toPact()
  }

  @PactSpecFor(pactMethod = 'articles', pactVersion = PactSpecVersion.V3)
  def 'fetches articles from the mock server'() {
    when:
    def http = new SimpleHttp(mockServer.url)
    def response = http.get('/articles')

    then:
    response.statusCode == 200
  }

  @PactSpecFor(pactMethod = 'users', pactVersion = PactSpecVersion.V3)
  def 'fetches users from the mock server'() {
    when:
    def http = new SimpleHttp(mockServer.url)
    def response = http.get('/users')

    then:
    response.statusCode == 200
  }
}
