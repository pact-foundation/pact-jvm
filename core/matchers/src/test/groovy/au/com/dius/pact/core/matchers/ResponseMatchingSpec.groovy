package au.com.dius.pact.core.matchers

import scala.None$
import scala.Some
import spock.lang.Specification

class ResponseMatchingSpec extends Specification {

  def 'response matching - match statuses'() {
    expect:
    Matching.matchStatus(200, 200) == None$.MODULE$
  }

  def 'response matching - mismatch statuses'() {
    expect:
    Matching.matchStatus(200, 300) == Some.apply(StatusMismatch.apply(200, 300))
  }
}
