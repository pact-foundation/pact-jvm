package au.com.dius.pact.provider

import spock.lang.Specification

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
//  def 'load pact files throws an exception if the directory is not readable'() {
//    given:
//    File dir = File.createTempDir()
//    dir.setReadable(false, false)
//    dir.deleteOnExit()
//
//    when:
//    ProviderUtils.loadPactFiles(providerInfo, dir)
//
//    then:
//    thrown(PactVerifierException)
//  }

}
