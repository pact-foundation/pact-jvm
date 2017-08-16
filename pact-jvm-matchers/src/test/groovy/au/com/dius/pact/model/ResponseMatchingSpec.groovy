package au.com.dius.pact.model

@SuppressWarnings('UnusedImport')
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
    Matching.matchStatus(200, 300) == Some.apply(new StatusMismatch(200, 300))
  }
}
