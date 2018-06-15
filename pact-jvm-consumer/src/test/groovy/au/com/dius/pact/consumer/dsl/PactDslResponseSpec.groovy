package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import com.google.common.net.MediaType
import org.apache.http.entity.ContentType
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.PactDslResponse.DEFAULT_JSON_CONTENT_TYPE_REGEX

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

  def 'default json content type should match common variants'() {
      def acceptableDefaultContentTypes = [
              'application/json;charset=utf-8',
              'application/json; charset=UTF-8',
              'application/json; charset=utf-8',

              ContentType.APPLICATION_JSON.toString(),
              MediaType.JSON_UTF_8.toString(),
      ]

      expect:
        acceptableDefaultContentTypes.each {
            it.matches(DEFAULT_JSON_CONTENT_TYPE_REGEX)
        }
  }

  def 'sets up any default state when created'() {
    given:
    ConsumerPactBuilder consumerPactBuilder = ConsumerPactBuilder.consumer('spec')
    PactDslRequestWithPath request = new PactDslRequestWithPath(consumerPactBuilder, 'spec', 'spec', [], 'test', '/',
      'GET', [:], [:], OptionalBody.empty(), new MatchingRulesImpl(), new Generators(), null, null)
    PactDslResponse defaultResponseValues = new PactDslResponse(consumerPactBuilder, request, null, null)
      .headers(['test': 'test'])
      .body('{"test":true}')
      .status(499)

    when:
    PactDslResponse subject = new PactDslResponse(consumerPactBuilder, request, null, defaultResponseValues)

    then:
    subject.responseStatus == 499
    subject.responseHeaders == [test: 'test']
    subject.responseBody == OptionalBody.body('{"test":true}')
  }

}
