package au.com.dius.pact.provider.junit5

import au.com.dius.pact.model.Consumer
import au.com.dius.pact.model.Provider
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class TestResultAccumulatorSpec extends Specification {

  static interaction1 = new RequestResponseInteraction('interaction1')
  static interaction2 = new RequestResponseInteraction('interaction2')
  static pact = new RequestResponsePact(new Provider('provider'), new Consumer('consumer'), [
    interaction1, interaction2
  ])

  @RestoreSystemProperties
  def 'lookupProviderVersion - returns the version set in the system properties'() {
    given:
    System.setProperty('pact.provider.version', '1.2.3')

    expect:
    TestResultAccumulator.INSTANCE.lookupProviderVersion() == '1.2.3'
  }

  def 'lookupProviderVersion - returns a default value if there is no version set in the system properties'() {
    expect:
    TestResultAccumulator.INSTANCE.lookupProviderVersion() == '0.0.0'
  }

  @Unroll
  def 'allInteractionsVerified returns #result when #condition'() {
    expect:
    TestResultAccumulator.INSTANCE.allInteractionsVerified(pact, results) == result

    where:

    condition                                           | results                                        | result
    'no results have been received'                     | [:]                                            | false
    'only some results have been received'              | [(interaction1): true]                         | false
    'all results have been received'                    | [(interaction1): true, (interaction2): true]   | true
    'all results have been received but some are false' | [(interaction1): true, (interaction2): false]  | false
    'all results have been received but all are false'  | [(interaction1): false, (interaction2): false] | false
  }

}
