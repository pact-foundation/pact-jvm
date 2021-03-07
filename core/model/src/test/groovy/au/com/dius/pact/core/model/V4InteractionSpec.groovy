package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification

class V4InteractionSpec extends Specification {
  def 'when downgrading message to V4, rename the matching rules from content to body'() {
    given:
    MatchingRules matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('content').addRule('$', TypeMatcher.INSTANCE)
    def message = new V4Interaction.AsynchronousMessage('key', 'description', OptionalBody.missing(),
      [:], matchingRules)

    when:
    def v3Message = message.asV3Interaction()

    then:
    v3Message.toMap(PactSpecVersion.V3).matchingRules == [body: ['$': [matchers: [[match: 'type']], combine: 'AND']]]
  }
}
