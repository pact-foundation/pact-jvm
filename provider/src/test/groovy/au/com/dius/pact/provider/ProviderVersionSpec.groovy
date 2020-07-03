package au.com.dius.pact.provider

import org.spockframework.lang.Wildcard
import spock.lang.Specification

class ProviderVersionSpec extends Specification {

  def cleanup() {
    System.clearProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT)
  }

  def 'provider version respects the property pact.provider.version.trimSnapshot'() {

    given:
    if (propertyValue != Wildcard.INSTANCE) {
      System.setProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT, propertyValue as String)
    }

    expect:
    new ProviderVersion({ projectVersion }).get() == result

    where:
    propertyValue  | projectVersion                              | result
    'true'         | '1.0.0-NOT-A-SNAPSHOT-abc-SNAPSHOT'         | '1.0.0-NOT-A-SNAPSHOT-abc'
    'true'         | '1.0.0-NOT-A-SNAPSHOT-abc-SNAPSHOT-re234hj' | '1.0.0-NOT-A-SNAPSHOT-abc-re234hj'
    'true'         | '1.0.0-SNAPSHOT-re234hj'                    | '1.0.0-re234hj'
    'false'        | '1.0.0-SNAPSHOT-re234hj'                    | '1.0.0-SNAPSHOT-re234hj'
    'aweirdstring' | '1.0.0-SNAPSHOT-re234hj'                    | '1.0.0-SNAPSHOT-re234hj'
    'true'         | null                                        | '0.0.0'
    'false'        | null                                        | '0.0.0'
    _              | '1.0.0-SNAPSHOT-re234hj'                    | '1.0.0-SNAPSHOT-re234hj'
  }
}
