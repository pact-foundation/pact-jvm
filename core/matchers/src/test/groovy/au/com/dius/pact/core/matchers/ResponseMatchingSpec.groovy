package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import spock.lang.Specification

class ResponseMatchingSpec extends Specification {
  private MatchingContext context

  def setup() {
    context = new MatchingContext(new MatchingRuleCategory('status'), false)
  }

  def 'response matching - match statuses'() {
    expect:
    Matching.INSTANCE.matchStatus(200, 200, context) == null
  }

  def 'response matching - mismatch statuses'() {
    expect:
    Matching.INSTANCE.matchStatus(200, 300, context) == new StatusMismatch(200, 300, null, [])
  }
}
