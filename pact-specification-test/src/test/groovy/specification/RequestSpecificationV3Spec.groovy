package specification

import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.RequestMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class RequestSpecificationV3Spec extends BaseRequestSpec {

  @Unroll
  def '#type #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).isEmpty() == match

    where:
    [type, test, match, matchDesc, expected, actual] << loadTestCases('/v3/request/', PactSpecVersion.V3)
  }

}
