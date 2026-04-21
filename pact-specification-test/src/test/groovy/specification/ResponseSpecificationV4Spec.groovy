package specification

import au.com.dius.pact.core.matchers.ResponseMatching
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import spock.lang.Unroll

class ResponseSpecificationV4Spec extends BaseResponseSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    given:
    def pact = new V4Pact(new Consumer(this.class.name), new Provider(this.class.name))
    def interaction = new V4Interaction.SynchronousHttp(
      matchDesc.toString(), [], new HttpRequest(), expected as HttpResponse)
    pact.interactions << interaction

    expect:
    ResponseMatching.responseMismatches(pact, interaction, actual).empty == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadV4TestCases('/v4/response/')
  }

}
