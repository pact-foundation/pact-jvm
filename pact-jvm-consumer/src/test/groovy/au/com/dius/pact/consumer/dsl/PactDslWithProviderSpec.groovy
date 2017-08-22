package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.model.ProviderState
import spock.lang.Specification

class PactDslWithProviderSpec extends Specification {

  def 'accept V3 provider state parameters'() {
    expect:
    interaction.providerStates == [ new ProviderState(state, params) ]

    where:
    state = 'a state with parameters'
    params = [valueA: '100', valueB: 'Bob']
    pact = ConsumerPactBuilder.consumer('v3-consumer')
      .hasPactWith('v3-service')
      .given(state, params)
      .uponReceiving('a request')
        .path('/request')
      .willRespondWith()
        .body(PactDslJsonRootValue.numberType())
      .toPact()
    interaction = pact.interactions.first()
  }

  def 'allow multiple states parameters'() {
    expect:
    interaction.providerStates == [ new ProviderState(state, params), new ProviderState(state2, params2) ]

    where:
    state = 'a state with parameters'
    state2 = 'another state with parameters'
    params = [valueA: '100', valueB: 'Bob']
    params2 = [valueC: new Date().toString()]
    pact = ConsumerPactBuilder.consumer('v3-consumer')
      .hasPactWith('v3-service')
      .given(state, params)
      .given(state2, params2)
      .uponReceiving('a request')
        .path('/request')
      .willRespondWith()
        .body(PactDslJsonRootValue.numberType())
      .toPact()
    interaction = pact.interactions.first()
  }

}
