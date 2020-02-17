package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

class PactBrokerSourceSpec extends Specification {

  @Unroll
  def 'description includes the URL for the Broker'() {
    expect:
    source.description() == description

    where:
    source                                                  | description
    new PactBrokerSource('localhost', '80', 'http')         | 'Pact Broker http://localhost:80'
    new PactBrokerSource('www.example.com', '443', 'https') | 'Pact Broker https://www.example.com:443'
  }
}
