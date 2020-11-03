package au.com.dius.pact.provider

import au.com.dius.pact.provider.junitsupport.Provider
import spock.lang.IgnoreIf
import spock.lang.Specification

@SuppressWarnings('UnnecessaryBooleanExpression')
@Provider('Test')
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

  def 'find annotation - can find an annotation on the test class'() {
    expect:
    ProviderUtils.findAnnotation(ProviderUtilsSpec, Provider).value() == 'Test'
  }

  @Provider('Parent')
  static class ParentClass { }

  static class TestClass extends ParentClass { }

  def 'find annotation - can find an annotation on the parent class'() {
    expect:
    ProviderUtils.findAnnotation(TestClass, Provider).value() == 'Parent'
  }

  @IsTestConsumer
  static class TestClass2 { }

  def 'find annotation - can find an annotation on annotations on the test class'() {
    expect:
    ProviderUtils.findAnnotation(TestClass2, Provider).value() == 'TestConsumer'
  }
}
