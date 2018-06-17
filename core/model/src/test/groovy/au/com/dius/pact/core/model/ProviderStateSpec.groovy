package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

class ProviderStateSpec extends Specification {

  @Unroll
  def 'generates a map of the state'() {
    expect:
    state.toMap() == map

    where:

    state                               | map
    new ProviderState('test')           | [name: 'test']
    new ProviderState('test', [:])      | [name: 'test']
    new ProviderState('test', [a: 'B']) | [name: 'test', params: [a: 'B']]
  }

}
