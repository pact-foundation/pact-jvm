package au.com.dius.pact.core.matchers

import spock.lang.Specification

class ResponseMatchingSpec extends Specification {

  def 'response matching - match statuses'() {
    expect:
    Matching.INSTANCE.matchStatus(200, 200) == null
  }

  def 'response matching - mismatch statuses'() {
    expect:
    Matching.INSTANCE.matchStatus(200, 300) == new StatusMismatch(200, 300)
  }
}
