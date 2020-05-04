package au.com.dius.pact.provider.junit

import au.com.dius.pact.provider.ProviderVerifier
import au.com.dius.pact.provider.junitsupport.loader.OverrideablePactLoader
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.JUnitProviderTestSupport
import au.com.dius.pact.provider.junitsupport.loader.PactLoader
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@AllowOverridePactUrl
@Consumer('Test')
class JUnitProviderTestSupportSpec extends Specification {

  private AllowOverridePactUrl allowOverridePactUrl
  private Consumer consumer

  def setup() {
    allowOverridePactUrl = JUnitProviderTestSupportSpec.getAnnotation(AllowOverridePactUrl)
    consumer = JUnitProviderTestSupportSpec.getAnnotation(Consumer)
  }

  def 'exceptionMessage should handle an exception with a null message'() {
    expect:
    JUnitProviderTestSupport.exceptionMessage(new NullPointerException(), 5) == 'null\n'
  }

  def 'checkForOverriddenPactUrl - does nothing if there is no pact loader'() {
    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(null, allowOverridePactUrl, null)

    then:
    noExceptionThrown()
  }

  def 'checkForOverriddenPactUrl - does nothing with a normal pact loader'() {
    given:
    PactLoader loader = Mock(PactLoader)

    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(loader, allowOverridePactUrl, null)

    then:
    0 * loader._
  }

  @RestoreSystemProperties
  def 'checkForOverriddenPactUrl - does nothing if there is no overridden pact annotation'() {
    given:
    PactLoader loader = Mock(OverrideablePactLoader)
    System.setProperty(ProviderVerifier.PACT_FILTER_PACTURL, 'http://overridden.url')

    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(loader, null, null)

    then:
    0 * loader._
  }

  @RestoreSystemProperties
  def 'checkForOverriddenPactUrl - does nothing if there is no consumer filter'() {
    given:
    PactLoader loader = Mock(OverrideablePactLoader)
    System.setProperty(ProviderVerifier.PACT_FILTER_PACTURL, 'http://overridden.url')

    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(loader, allowOverridePactUrl, null)

    then:
    0 * loader._
  }

  @RestoreSystemProperties
  def 'checkForOverriddenPactUrl - uses the consumer annotation'() {
    given:
    PactLoader loader = Mock(OverrideablePactLoader)
    System.setProperty(ProviderVerifier.PACT_FILTER_PACTURL, 'http://overridden.url')

    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(loader, allowOverridePactUrl, consumer)

    then:
    1 * loader.overridePactUrl('http://overridden.url', 'Test')
  }

  @RestoreSystemProperties
  def 'checkForOverriddenPactUrl - falls back to the consumer filter property'() {
    given:
    PactLoader loader = Mock(OverrideablePactLoader)
    System.setProperty(ProviderVerifier.PACT_FILTER_PACTURL, 'http://overridden.url')
    System.setProperty(ProviderVerifier.PACT_FILTER_CONSUMERS, 'Bob')

    when:
    JUnitProviderTestSupport.checkForOverriddenPactUrl(loader, allowOverridePactUrl, null)

    then:
    1 * loader.overridePactUrl('http://overridden.url', 'Bob')
  }

}
