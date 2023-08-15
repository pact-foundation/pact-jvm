package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

@SuppressWarnings('LineLength')
class MatcherExecutorKtSpec extends Specification {

  def 'match regex'() {
    expect:
    MatcherExecutorKt.matchRegex(regex, [], '', actual, { a, b, c, d -> new HeaderMismatch('test', '', actual, c) } as MismatchFactory) == result

    where:

    regex                           | actual          | result
    'look|look_bordered|slider_cta' | 'look_bordered' | []
  }

  def 'match semver'() {
    expect:
    MatcherExecutorKt.matchSemver(['$'], '1.2.3', actual, { a, b, c, d -> new HeaderMismatch('test', '', actual.toString(), c) } as MismatchFactory) == result

    where:

    actual                             | result
    '4.5.7'                            | []
    '4.5.7.8'                          | [new HeaderMismatch('test', '', '4.5.7.8', "'4.5.7.8' is not a valid semantic version")]
    '04.5.7'                           | [new HeaderMismatch('test', '', '04.5.7', "'04.5.7' is not a valid semantic version")]
    new JsonValue.StringValue('4.5.7') | []
  }
}
