package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings(['AbcMetric', 'ExplicitCallToOrMethod', 'UnnecessaryObjectReferences'])
class PactSpecVersionSpec extends Specification {
  def 'version string'() {
    expect:
    version.versionString() == result

    where:

    version                     | result
    PactSpecVersion.UNSPECIFIED | '3.0.0'
    PactSpecVersion.V1          | '1.0.0'
    PactSpecVersion.V1_1        | '1.1.0'
    PactSpecVersion.V2          | '2.0.0'
    PactSpecVersion.V3          | '3.0.0'
    PactSpecVersion.V4          | '4.0'
  }

  def 'or'() {
    expect:
    version.or(otherVersion) == result

    where:

    version                     | otherVersion                | result
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V3
    PactSpecVersion.V1          | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.UNSPECIFIED | PactSpecVersion.V4
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.V1          | PactSpecVersion.V1
    PactSpecVersion.V1          | PactSpecVersion.V1          | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.V1          | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.V1          | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.V1          | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.V1          | PactSpecVersion.V4
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.V1_1        | PactSpecVersion.V1_1
    PactSpecVersion.V1          | PactSpecVersion.V1_1        | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.V1_1        | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.V1_1        | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.V1_1        | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.V1_1        | PactSpecVersion.V4
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.V2          | PactSpecVersion.V2
    PactSpecVersion.V1          | PactSpecVersion.V2          | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.V2          | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.V2          | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.V2          | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.V2          | PactSpecVersion.V4
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.V3          | PactSpecVersion.V3
    PactSpecVersion.V1          | PactSpecVersion.V3          | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.V3          | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.V3          | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.V3          | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.V3          | PactSpecVersion.V4
    PactSpecVersion.UNSPECIFIED | PactSpecVersion.V4          | PactSpecVersion.V4
    PactSpecVersion.V1          | PactSpecVersion.V4          | PactSpecVersion.V1
    PactSpecVersion.V1_1        | PactSpecVersion.V4          | PactSpecVersion.V1_1
    PactSpecVersion.V2          | PactSpecVersion.V4          | PactSpecVersion.V2
    PactSpecVersion.V3          | PactSpecVersion.V4          | PactSpecVersion.V3
    PactSpecVersion.V4          | PactSpecVersion.V4          | PactSpecVersion.V4
  }

  def 'from int'() {
    expect:
    PactSpecVersion.fromInt(intValue) == version

    where:

    intValue | version
    0        | PactSpecVersion.V3
    1        | PactSpecVersion.V1
    2        | PactSpecVersion.V2
    3        | PactSpecVersion.V3
    4        | PactSpecVersion.V4
    5        | PactSpecVersion.V3
  }

  @RestoreSystemProperties
  def 'default version'() {
    given:
    System.setProperty('pact.defaultVersion', version)

    expect:
    PactSpecVersion.defaultVersion() == expected

    where:

    version | expected
    ''      | PactSpecVersion.V3
    'V1'    | PactSpecVersion.V1
    'V1_1'  | PactSpecVersion.V1_1
    'V2'    | PactSpecVersion.V2
    'V3'    | PactSpecVersion.V3
    'V4'    | PactSpecVersion.V4
  }

  @RestoreSystemProperties
  def 'default version when not set'() {
    given:
    System.clearProperty('pact.defaultVersion')

    expect:
    PactSpecVersion.defaultVersion() == PactSpecVersion.V3
  }

  @RestoreSystemProperties
  def 'default version when invalid'() {
    given:
    System.setProperty('pact.defaultVersion', 'invalid')

    when:
    PactSpecVersion.defaultVersion()

    then:
    thrown(IllegalArgumentException)
  }
}
