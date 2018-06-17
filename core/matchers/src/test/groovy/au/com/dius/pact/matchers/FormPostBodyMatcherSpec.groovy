package au.com.dius.pact.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification

class FormPostBodyMatcherSpec extends Specification {

  private FormPostBodyMatcher matcher
  private MatchingRulesImpl matchers
  private final expected = { body -> new Request(body: body, matchingRules: matchers) }
  private final actual = { body -> new Request(body: body) }

  def setup() {
    matcher = new FormPostBodyMatcher()
    matchers = new MatchingRulesImpl()
  }

  def 'returns no mismatches - when the expected body is missing'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.missing()
  }

  def 'returns no mismatches - when the expected body and actual bodies are empty'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.empty()
    expectedBody = OptionalBody.empty()
  }

  def 'returns no mismatches - when the expected body and actual bodies are equal'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.body('a=b&c=d')
    expectedBody = OptionalBody.body('a=b&c=d')
  }

  def 'returns no mismatches - when the actual body has extra keys and we allow unexpected keys'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.body('a=b&c=d')
    expectedBody = OptionalBody.body('a=b')
  }

  def 'returns no mismatches - when the keys are in different order'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.body('a=b&c=d')
    expectedBody = OptionalBody.body('c=d&a=b')
  }

  def 'returns mismatches - when the expected body contains keys that are not in the actual body'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      ['Expected form post parameter \'c\' but was missing']

    where:
    actualBody = OptionalBody.body('a=b')
    expectedBody = OptionalBody.body('a=b&c=d')
  }

  @SuppressWarnings('LineLength')
  def 'returns mismatches - when the actual body contains keys that are not in the expected body and we do not allow extra keys'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), false)*.mismatch ==
      ['Received unexpected form post parameter \'a\'=[\'b\']']

    where:
    actualBody = OptionalBody.body('a=b&c=d')
    expectedBody = OptionalBody.body('c=d')
  }

  def 'returns mismatches - when the expected body is present but there is no actual body'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      ['Expected a form post body but was missing']

    where:
    actualBody = OptionalBody.missing()
    expectedBody = OptionalBody.body('a=a')
  }

  def 'returns mismatches - if the same key is repeated with values in different order'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      [
        'Expected form post parameter \'a\'[0] with value \'1\' but was \'2\'',
        'Expected form post parameter \'a\'[1] with value \'2\' but was \'1\''
      ]

    where:
    actualBody = OptionalBody.body('a=2&a=1&b=3')
    expectedBody = OptionalBody.body('a=1&a=2&b=3')
  }

  def 'returns mismatches - if the same key is repeated with values missing'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      [
        'Expected form post parameter \'a\'=\'3\' but was missing'
      ]

    where:
    actualBody = OptionalBody.body('a=1&a=2')
    expectedBody = OptionalBody.body('a=1&a=2&a=3')
  }

  def 'returns mismatches - when the actual body contains values that are not the same as the expected body'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      ['Expected form post parameter \'c\'[0] with value \'d\' but was \'1\'']

    where:
    actualBody = OptionalBody.body('a=b&c=1')
    expectedBody = OptionalBody.body('c=d&a=b')
  }

  def 'handles delimiters in the values'() {
    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true)*.mismatch ==
      ['Expected form post parameter \'c\'[0] with value \'1\' but was \'1=2\'']

    where:
    actualBody = OptionalBody.body('a=b&c=1=2')
    expectedBody = OptionalBody.body('c=1&a=b')
  }

  def 'delegates to any defined matcher'() {
    given:
    matchers.addCategory('body').addRule('$.c', TypeMatcher.INSTANCE)

    expect:
    matcher.matchBody(expected(expectedBody), actual(actualBody), true).empty

    where:
    actualBody = OptionalBody.body('a=b&c=2')
    expectedBody = OptionalBody.body('c=1&a=b')
  }
}
