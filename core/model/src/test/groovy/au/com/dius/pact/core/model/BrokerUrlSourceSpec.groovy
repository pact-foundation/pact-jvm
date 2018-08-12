package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

class BrokerUrlSourceSpec extends Specification {

  @Unroll
  def 'description includes the tag if one is set'() {
    expect:
    source.description() == description

    where:
    source                                                    | description
    new BrokerUrlSource('URL', 'Broker URL')                  | 'Pact Broker URL'
    new BrokerUrlSource('URL', 'Broker URL', [:], [:], 'TAG') | 'Pact Broker URL (Tag TAG)'
  }
}
