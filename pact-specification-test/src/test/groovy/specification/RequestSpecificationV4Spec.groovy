package specification

import au.com.dius.pact.core.matchers.RequestMatching
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Unroll

class RequestSpecificationV4Spec extends BaseRequestSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    given:
    def pact = new V4Pact(new Consumer(this.class.name), new Provider(this.class.name))
    def interaction = new V4Interaction.SynchronousHttp(null, matchDesc.toString(), [], expected)
    pact.interactions << interaction

    expect:
    RequestMatching.requestMismatches(pact, interaction, actual).matchedOk() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadV4TestCases('/v4/request/')
  }
}
