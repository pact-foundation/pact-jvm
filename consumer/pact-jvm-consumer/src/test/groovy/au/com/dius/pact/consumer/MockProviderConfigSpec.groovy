package au.com.dius.pact.consumer

import au.com.dius.pact.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import spock.lang.Specification

class MockProviderConfigSpec extends Specification {
  def "port server config - select a random port"() {
    expect:
    MockProviderConfig.createDefault(PactSpecVersion.V3).port >= MockProviderConfig.portLowerBound
    MockProviderConfig.createDefault(PactSpecVersion.V3).port <= MockProviderConfig.portUpperBound
  }
}
