package au.com.dius.pact.consumer.spock

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.support.SimpleHttp
import spock.lang.Specification

/**
 * Tests that @PactSpecFor placed on the class applies to all feature methods.
 */
@SuppressWarnings(['JUnitPublicNonTestMethod', 'FactoryMethodName'])
@PactConsumerSpockTest
@PactSpecFor(providerName = 'class_level_provider', pactMethod = 'classPact', pactVersion = PactSpecVersion.V3)
class ClassLevelAnnotationSpec extends Specification {

  MockServer mockServer

  @Pact(provider = 'class_level_provider', consumer = 'spock_consumer')
  RequestResponsePact classPact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request')
        .path('/hello')
        .method('GET')
      .willRespondWith()
        .status(200)
        .body('{"message":"hello"}')
        .headers(['Content-Type': 'application/json'])
      .toPact()
  }

  def 'uses class-level pact annotation'() {
    when:
    def http = new SimpleHttp(mockServer.url)
    def response = http.get('/hello')

    then:
    response.statusCode == 200
  }
}
