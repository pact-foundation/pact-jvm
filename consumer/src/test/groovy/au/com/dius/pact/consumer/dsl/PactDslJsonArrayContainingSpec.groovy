package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import kotlin.Triple
import spock.lang.Specification

class PactDslJsonArrayContainingSpec extends Specification {
  private PactDslJsonBody parent

  def setup() {
    parent = new PactDslJsonBody()
  }

  def 'with one variant'() {
    given:
    def array = new PactDslJsonArrayContaining('.', 'array', parent)
      .stringType()

    when:
    def result = array.close()

    then:
    result.toString() == '{"array":["string"]}'
    result.matchers.matchingRules == [
      '$.array': new MatchingRuleGroup([
        new ArrayContainsMatcher([
          new Triple(
            0,
            new MatchingRuleCategory('body', [
              '$': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE])
            ]),
            ['$': new RandomStringGenerator(20)]
          )
        ])
      ])
    ]
    result.generators == new Generators([:])
  }

  def 'with two variants'() {
    given:
    def array = new PactDslJsonArrayContaining('.', 'array', parent)
      .stringType()
      .numberType()

    when:
    def result = array.close()

    then:
    result.toString() == '{"array":["string",100]}'
    result.matchers.matchingRules == [
      '$.array': new MatchingRuleGroup([
        new ArrayContainsMatcher([
          new Triple(
            0,
            new MatchingRuleCategory('body', [
              '$': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE])
            ]),
            ['$': new RandomStringGenerator(20)]
          ),
          new Triple(
            1,
            new MatchingRuleCategory('body', [
              '$': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)])
            ]),
            ['$': new RandomIntGenerator(0, Integer.MAX_VALUE)]
          )
        ])
      ])
    ]
    result.generators == new Generators([:])
  }

  def 'with one primitive variant'() {
    given:
    def array = new PactDslJsonArrayContaining('.', 'array', parent)
      .stringValue('a')

    when:
    def result = array.close()

    then:
    result.toString() == '{"array":["a"]}'
    result.matchers.matchingRules == [
      '$.array': new MatchingRuleGroup([
        new ArrayContainsMatcher([
          new Triple(
            0,
            new MatchingRuleCategory('body', [:]),
            [:]
          )
        ])
      ])
    ]
    result.generators == new Generators([:])
  }

  def 'with two primitive variants'() {
    given:
    def array = new PactDslJsonArrayContaining('.', 'array', parent)
      .stringValue('a')
      .numberValue(100)

    when:
    def result = array.close()

    then:
    result.toString() == '{"array":["a",100]}'
    result.matchers.matchingRules == [
      '$.array': new MatchingRuleGroup([
        new ArrayContainsMatcher([
          new Triple(
            0,
            new MatchingRuleCategory('body', [:]),
            [:]
          ),
          new Triple(
            1,
            new MatchingRuleCategory('body', [:]),
            [:]
          )
        ])
      ])
    ]
    result.generators == new Generators([:])
  }
}
