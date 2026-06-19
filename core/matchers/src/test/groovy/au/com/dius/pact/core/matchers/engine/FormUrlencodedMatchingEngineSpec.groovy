package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import spock.lang.Specification

class FormUrlencodedMatchingEngineSpec extends Specification {
  def 'matches form-urlencoded body with type and eachValue body matchers'() {
    given:
    def actualRequest = new HttpRequest('POST', '/form', [:], [:],
      OptionalBody.body('id=200&tags=1&tags=2', new ContentType('application/x-www-form-urlencoded')))

    def bodyRules = new MatchingRuleCategory('body')
    bodyRules
      .addRule('$.id', TypeMatcher.INSTANCE)
      .addRule('$.tags', new EachValueMatcher(new MatchingRuleDefinition(
        '1',
        TypeMatcher.INSTANCE,
        null,
        'matching(type, 1)'
      )))
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory(bodyRules)

    def expectedRequest = new HttpRequest('POST', '/form', [:], [:],
      OptionalBody.body('id=100&tags=1', new ContentType('application/x-www-form-urlencoded')),
      matchingRules, new Generators())

    def pact = new V4Pact(new Consumer('test-consumer'), new Provider('test-provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test interaction', [], expectedRequest)
    def context = new PlanMatchingContext(pact, interaction, new MatchingConfiguration(false, false, true, false))

    when:
    def plan = V2MatchingEngine.INSTANCE.buildRequestPlan(expectedRequest, context)
    def executedPlan = V2MatchingEngine.INSTANCE.executeRequestPlan(plan, actualRequest, context)

    then:
    executedPlan.intoRequestMatchResult().matchedOk()
  }
}
