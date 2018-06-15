package au.com.dius.pact.model

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import spock.lang.Specification
import spock.lang.Unroll

class ProviderAndConsumerSpec extends Specification {

  @Unroll
  def 'creates a provider from a Map'() {
    expect:
    Provider.fromMap(map) == provider

    where:

    map            | provider
    [:]            | new Provider('provider')
    [name: null]   | new Provider('provider')
    [name: '']     | new Provider('provider')
    [name: 'test'] | new Provider('test')
  }

  @Unroll
  def 'creates a consumer from a Map'() {
    expect:
    Consumer.fromMap(map) == consumer

    where:

    map            | consumer
    [:]            | new Consumer('consumer')
    [name: null]   | new Consumer('consumer')
    [name: '']     | new Consumer('consumer')
    [name: 'test'] | new Consumer('test')
  }

}
