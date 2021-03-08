package specification

import au.com.dius.pact.core.matchers.RequestMatching
import spock.lang.Unroll

class RequestSpecificationV4Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).matchedOk() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadV4TestCases('/v4/request/')
  }
}
