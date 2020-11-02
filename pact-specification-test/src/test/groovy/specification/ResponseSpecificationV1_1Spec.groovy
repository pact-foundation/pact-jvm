package specification

import au.com.dius.pact.core.matchers.ResponseMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class ResponseSpecificationV1_1Spec extends BaseResponseSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    ResponseMatching.responseMismatches(expected, actual).empty == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v1.1/response/')
  }

}
