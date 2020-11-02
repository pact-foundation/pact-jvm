package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import spock.lang.Specification

class TypeMatcherSpec extends Specification {

  def 'match integers should accept integer values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = OptionalBody.body('{"value": 123}'.bytes)
    def actual = OptionalBody.body('{"value": 456}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    result.mismatches.empty
  }

  def 'match integers should not match null values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = OptionalBody.body('{"value": 123}'.bytes)
    def actual = OptionalBody.body('{"value": null}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    !result.mismatches.empty
  }

  def 'match integers should fail for non-integer values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = OptionalBody.body('{"value": 123}'.bytes)
    def actual = OptionalBody.body('{"value": 123.10}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    !result.mismatches.empty
  }

  def 'match decimal should accept decimal values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = OptionalBody.body('{"value": 123.10}'.bytes)
    def actual = OptionalBody.body('{"value": 456.20}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    result.mismatches.empty
  }

  def 'match decimal should handle null values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = OptionalBody.body('{"value": 123.10}'.bytes)
    def actual = OptionalBody.body('{"value": null}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    !result.mismatches.empty
  }

  def 'match decimal should fail for non-decimal values'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = OptionalBody.body('{"value": 123.10}'.bytes)
    def actual = OptionalBody.body('{"value": 123}'.bytes)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, context)

    then:
    !result.mismatches.empty
  }
}
