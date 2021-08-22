package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.ProviderState
import spock.lang.Issue
import spock.lang.Specification

class ConsumerPactBuilderSpec extends Specification {
  @Issue('#497')
  def 'previous provider states must not be copied over to new interactions'() {
    given:
      def pact = ConsumerPactBuilder.consumer('test')
        .hasPactWith('provider')
        .given('greeting', [name: 'world'])
        .uponReceiving('GET /hello')
        .path('/hello')
        .method('POST')
        .willRespondWith()
        .status(200)
        .uponReceiving('GET /hello-user')
        .path('/hello-user')
        .method('POST')
        .willRespondWith()
        .status(200)
        .toPact()

    expect:
      pact.interactions[0].providerStates == [new ProviderState('greeting', [name: 'world'])]
      pact.interactions[1].providerStates == []
  }
}
