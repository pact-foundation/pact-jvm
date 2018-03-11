package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

class FeatureTogglesSpec extends Specification {

  def setup() {
    FeatureToggles.toggleFeature('pact.test', true)
    FeatureToggles.toggleFeature('pact.test.2', false)
  }

  def cleanup() {
    FeatureToggles.reset()
  }

  @Unroll
  def 'feature toggle test'() {
    expect:
    FeatureToggles.isFeatureSet(name) == featureSet

    where:

    name                                          | featureSet
    ''                                            | false
    'pact'                                        | false
    'pact.feature.matchers.useMatchValuesMatcher' | false
    'pact.test'                                   | true
    'pact.test.2'                                 | false
  }

}
