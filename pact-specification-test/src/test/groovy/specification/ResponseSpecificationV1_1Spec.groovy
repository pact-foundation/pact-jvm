package specification

import au.com.dius.pact.core.matchers.ResponseMatching
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class ResponseSpecificationV1_1Spec extends BaseResponseSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    given:
    def pact = new RequestResponsePact(new Provider(this.class.name), new Consumer(this.class.name))
    def interaction = new RequestResponseInteraction(matchDesc.toString(), [], new Request(), expected as Response)
    pact.interactions << interaction

    expect:
    ResponseMatching.responseMismatches(pact, interaction, actual).empty == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v1.1/response/')
  }

}
