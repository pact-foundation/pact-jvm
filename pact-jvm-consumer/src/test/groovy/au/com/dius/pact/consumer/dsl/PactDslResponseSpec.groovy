package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.model.matchingrules.TypeMatcher
import spock.lang.Specification

class PactDslResponseSpec extends Specification {

  def 'allow matchers to be set at root level'() {
    expect:
    response.matchingRules.rulesForCategory('body').matchingRules == [
      '$': new MatchingRuleGroup([TypeMatcher.INSTANCE])]

    where:
    pact = ConsumerPactBuilder.consumer('complex-instruction-service')
      .hasPactWith('workflow-service')
      .uponReceiving('a request to start a workflow')
      .path('/startWorkflowProcessInstance')
      .willRespondWith()
      .body(PactDslJsonRootValue.numberType())
      .toPact()
    interaction = pact.interactions.first()
    response = interaction.response
  }

}
