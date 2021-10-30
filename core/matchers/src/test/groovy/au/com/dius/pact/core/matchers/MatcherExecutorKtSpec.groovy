package au.com.dius.pact.core.matchers

import spock.lang.Specification

class MatcherExecutorKtSpec extends Specification {

  def 'match regex'() {
    expect:
    MatcherExecutorKt.matchRegex(regex, [], '', actual, { a, b, c, d -> new HeaderMismatch('test', '', actual, c) } as MismatchFactory) == result

    where:

    regex                           | actual          | result
    'look|look_bordered|slider_cta' | 'look_bordered' | []
  }

}
