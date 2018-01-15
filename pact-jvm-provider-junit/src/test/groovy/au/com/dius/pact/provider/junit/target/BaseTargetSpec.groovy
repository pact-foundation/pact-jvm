package au.com.dius.pact.provider.junit.target

import spock.lang.Specification

class BaseTargetSpec extends Specification {

  def 'exceptionMessage should handle an exception with a null message'() {
    expect:
    BaseTarget.exceptionMessage(new NullPointerException(), 5) == 'null\n'
  }

}
