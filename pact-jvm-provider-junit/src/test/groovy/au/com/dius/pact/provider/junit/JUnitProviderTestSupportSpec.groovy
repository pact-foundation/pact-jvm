package au.com.dius.pact.provider.junit

import spock.lang.Specification

class JUnitProviderTestSupportSpec extends Specification {

  def 'exceptionMessage should handle an exception with a null message'() {
    expect:
    JUnitProviderTestSupport.exceptionMessage(new NullPointerException(), 5) == 'null\n'
  }

}
