package au.com.dius.pact.provider

import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings('UnnecessaryBooleanExpression')
class ProviderUtilsSpec extends Specification {

  private ProviderInfo providerInfo

  def setup() {
    providerInfo = new ProviderInfo('Bob')
  }

  def 'load pact files throws an exception if the directory does not exist'() {
    given:
    File dir = new File('/this/does/not/exist')

    when:
    ProviderUtils.loadPactFiles(providerInfo, dir)

    then:
    thrown(PactVerifierException)
  }

  def 'load pact files throws an exception if the directory is not a directory'() {
    given:
    File dir = new File('README.md')

    when:
    ProviderUtils.loadPactFiles(providerInfo, dir)

    then:
    thrown(PactVerifierException)
  }

  // Fails on windows
  @IgnoreIf({ os.windows })
  def 'load pact files throws an exception if the directory is not readable'() {
    given:
    File dir = File.createTempDir()
    dir.setReadable(false, false)
    dir.deleteOnExit()

    when:
    ProviderUtils.loadPactFiles(providerInfo, dir)

    then:
    thrown(PactVerifierException)
  }

  @SuppressWarnings('LineLength')
  def 'verification type test'() {
    expect:
    ProviderUtils.verificationType(provider, consumer) == verificationType

    where:
    provider                                                              | consumer                                                              || verificationType
    new ProviderInfo()                                                    | new ConsumerInfo()                                                    || PactVerification.REQUEST_RESPONSE
    new ProviderInfo()                                                    | new ConsumerInfo(verificationType: PactVerification.ANNOTATED_METHOD) || PactVerification.ANNOTATED_METHOD
    new ProviderInfo(verificationType: PactVerification.REQUEST_RESPONSE) | new ConsumerInfo(verificationType: PactVerification.ANNOTATED_METHOD) || PactVerification.ANNOTATED_METHOD
    new ProviderInfo(verificationType: PactVerification.ANNOTATED_METHOD) | new ConsumerInfo()                                                    || PactVerification.ANNOTATED_METHOD
  }

  def 'packages to scan test'() {
    expect:
    ProviderUtils.packagesToScan(provider, consumer) == packagesToScan

    where:
    provider | consumer || packagesToScan
    new ProviderInfo() | new ConsumerInfo() || []
    new ProviderInfo() | new ConsumerInfo(packagesToScan: ['a.b.c']) || ['a.b.c']
    new ProviderInfo(packagesToScan: ['d.e.f']) | new ConsumerInfo(packagesToScan: ['a.b.c']) || ['a.b.c']
    new ProviderInfo(packagesToScan: ['d.e.f']) | new ConsumerInfo() || ['d.e.f']
  }

  @RestoreSystemProperties
  def 'provider versions with trim snapshot property true' () {

    expect:
    ProviderUtils.getProviderVersion(projectVersion) == result

    where:
    systemProperty | projectVersion || result
    System.setProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT, 'true') |
            '1.0.0-SNAPSHOT-re234hj' |
            '1.0.0-re234hj'
  }

  @RestoreSystemProperties
  def 'provider versions with trim snapshot property false' () {

    expect:
    ProviderUtils.getProviderVersion(projectVersion) == result

    where:
    systemProperty | projectVersion || result
    System.setProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT, 'false') |
            '1.0.0-SNAPSHOT-re234hj' |
            '1.0.0-SNAPSHOT-re234hj'
  }

  @RestoreSystemProperties
  def 'provider versions with trim snapshot property wrong value' () {

    expect:
    ProviderUtils.getProviderVersion(projectVersion) == result

    where:
    systemProperty | projectVersion || result
    System.setProperty(ProviderVerifier.PACT_PROVIDER_VERSION_TRIM_SNAPSHOT, 'aweirdstring') |
            '1.0.0-SNAPSHOT-re234hj' |
            '1.0.0-SNAPSHOT-re234hj'
  }

  @RestoreSystemProperties
  def 'provider versions with trim snapshot property not set' () {

    expect:
    ProviderUtils.getProviderVersion(projectVersion) == result

    where:
    systemProperty | projectVersion || result
    _ | '1.0.0-SNAPSHOT-re234hj' | '1.0.0-SNAPSHOT-re234hj'
  }

}
