package specification

import au.com.dius.pact.core.matchers.RequestMatching
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class RequestSpecificationV1_1Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name #test #matchDesc'() {
    given:
    def pact = new RequestResponsePact(new Provider(this.class.name), new Consumer(this.class.name))
    def interaction = new RequestResponseInteraction(matchDesc.toString(), [], expected)
    pact.interactions << interaction

    expect:
    RequestMatching.requestMismatches(pact, interaction, actual).matchedOk() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v1.1/request/')
  }
}
