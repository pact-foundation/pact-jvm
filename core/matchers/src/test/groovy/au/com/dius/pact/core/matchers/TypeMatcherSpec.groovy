package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import spock.lang.Specification

class TypeMatcherSpec extends Specification {

  private final Boolean allowUnexpectedKeys = true

  def 'match integers should accept integer values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 456}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    result.empty
  }

  def 'match integers should not match null values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": null}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match integers should fail for non-integer values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match decimal should accept decimal values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 456.20}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    result.empty
  }

  def 'match decimal should handle null values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": null}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

  def 'match decimal should fail for non-decimal values'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL))
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": 123.10}'.bytes), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": 123}'.bytes), null)

    when:
    def result = new JsonBodyMatcher().matchBody(expected, actual, allowUnexpectedKeys)

    then:
    !result.empty
  }

}
