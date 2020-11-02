package specification

import au.com.dius.pact.core.matchers.ResponseMatching
import spock.lang.Unroll

class ResponseSpecificationV3Spec extends BaseResponseSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    ResponseMatching.responseMismatches(expected, actual).empty == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v3/response/')
  }

}
