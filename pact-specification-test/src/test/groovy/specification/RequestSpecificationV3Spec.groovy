package specification

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.model.RequestMatching
import spock.lang.Unroll

class RequestSpecificationV3Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).isEmpty() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v3/request/', PactSpecVersion.V3)
  }

}
