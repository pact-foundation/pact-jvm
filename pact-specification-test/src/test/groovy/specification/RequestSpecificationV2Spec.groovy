package specification

import au.com.dius.pact.core.matchers.RequestMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class RequestSpecificationV2Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).matchedOk() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v2/request/')
  }

}
