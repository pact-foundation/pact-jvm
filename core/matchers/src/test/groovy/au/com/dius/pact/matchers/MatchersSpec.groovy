package au.com.dius.pact.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.model.Request
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification

class MatchersSpec extends Specification {

  def 'matchers defined - should be false when there are no matchers'() {
    expect:
    !Matchers.matcherDefined('body', [''], new MatchingRulesImpl())
  }

  def 'matchers defined - should be false when the path does not have a matcher entry'() {
    expect:
    !Matchers.matcherDefined('body', ['$', 'something'], new MatchingRulesImpl())
  }

  def 'matchers defined - should be true when the path does have a matcher entry'() {
    expect:
    Matchers.matcherDefined('body', ['$', 'something'], matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body').addRule('$.something', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'matchers defined - should be true when a parent of the path has a matcher entry'() {
    expect:
    Matchers.matcherDefined('body', ['$', 'something'], matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body').addRule('$', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'wildcardMatcherDefined - should be false when there are no matchers'() {
    expect:
    !Matchers.wildcardMatcherDefined([''], 'body', new MatchingRulesImpl())
  }

  def 'wildcardMatcherDefined - should be false when the path does not have a matcher entry'() {
    expect:
    !Matchers.wildcardMatcherDefined(['$', 'something'], 'body', new MatchingRulesImpl())
  }

  def 'wildcardMatcherDefined - should be false when the path does have a matcher entry and it is not a wildcard'() {
    expect:
    !Matchers.wildcardMatcherDefined(['$', 'some', 'thing'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.some.thing', TypeMatcher.INSTANCE)
        .addRule('$.*', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'wildcardMatcherDefined - should be true when the path does have a matcher entry and it is a wildcard'() {
    expect:
    Matchers.wildcardMatcherDefined(['$', 'something'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.*', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'wildcardMatcherDefined - should be false when a parent of the path has a matcher entry'() {
    expect:
    !Matchers.wildcardMatcherDefined(['$', 'some', 'thing'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.*', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'should default to a matching defined at a parent level'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$', TypeMatcher.INSTANCE)

    when:
    def rules = Matchers.selectBestMatcher(matchingRules, 'body', ['$', 'value'])

    then:
    rules.rules.first() == TypeMatcher.INSTANCE
  }

  def 'with matching rules with the same weighting, select the one of the same path length'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body')
      .addRule('$.rawArray', TypeMatcher.INSTANCE)
      .addRule('$.rawArrayEqTo', TypeMatcher.INSTANCE)
      .addRule('$.rawArrayEqTo[*]', EqualsMatcher.INSTANCE)
      .addRule('$.regexpRawArray', TypeMatcher.INSTANCE)
      .addRule('$.regexpRawArray[*]', new RegexMatcher('.+'))

    when:
    def rules = Matchers.selectBestMatcher(matchingRules, 'body', ['$', 'rawArrayEqTo', '1'])

    then:
    rules.rules == [ EqualsMatcher.INSTANCE ]
  }

  def 'type matcher - match on type - list elements should inherit the matcher from the parent'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', TypeMatcher.INSTANCE)
    def expected = new Request('get', '/', null, null, OptionalBody.body('{"value": [100]}'), matchingRules)
    def actual = new Request('get', '/', null, null, OptionalBody.body('{"value": ["200.3"]}'), null)

    when:
    def mismatches = new JsonBodyMatcher().matchBody(expected, actual, true)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected \'200.3\' to be the same type as 100']
  }

  def 'type matcher - match on type - map elements should inherit the matchers from the parent'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('body').addRule('$.value', TypeMatcher.INSTANCE)
    def expected = new Request('get', '/', null, null,
      OptionalBody.body('{"value": {"a": 100}}'), matchingRules)
    def actual = new Request('get', '/', null, null,
      OptionalBody.body('{"value": {"a": "200.3", "b": 200, "c": 300} }'), null)

    when:
    def mismatches = new JsonBodyMatcher().matchBody(expected, actual, true)

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected \'200.3\' to be the same type as 100']
  }

  def 'path matching - match root node'() {
    expect:
    Matchers.INSTANCE.matchesPath('$', ['$']) > 0
    Matchers.INSTANCE.matchesPath('$', []) == 0
  }

  def 'path matching - match field name'() {
    expect:
    Matchers.INSTANCE.matchesPath('$.name', ['$', 'name']) > 0
    Matchers.INSTANCE.matchesPath('$.name.other', ['$', 'name', 'other']) > 0
    Matchers.INSTANCE.matchesPath('$.name', ['$', 'other']) == 0
    Matchers.INSTANCE.matchesPath('$.name', ['$', 'name', 'other']) > 0
    Matchers.INSTANCE.matchesPath('$.other', ['$', 'name', 'other']) == 0
    Matchers.INSTANCE.matchesPath('$.name.other', ['$', 'name']) == 0
  }

  def 'path matching - match array indices'() {
    expect:
    Matchers.INSTANCE.matchesPath('$[0]', ['$', '0']) > 0
    Matchers.INSTANCE.matchesPath('$.name[1]', ['$', 'name', '1']) > 0
    Matchers.INSTANCE.matchesPath('$.name', ['$', '0']) == 0
    Matchers.INSTANCE.matchesPath('$.name[1]', ['$', 'name', '0']) == 0
    Matchers.INSTANCE.matchesPath('$[1].name', ['$', 'name', '1']) == 0
  }

  def 'path matching - match with wildcard'() {
    expect:
    Matchers.INSTANCE.matchesPath('$[*]', ['$', '0']) > 0
    Matchers.INSTANCE.matchesPath('$.*', ['$', 'name']) > 0
    Matchers.INSTANCE.matchesPath('$.*.name', ['$', 'some', 'name']) > 0
    Matchers.INSTANCE.matchesPath('$.name[*]', ['$', 'name', '0']) > 0
    Matchers.INSTANCE.matchesPath('$.name[*].name', ['$', 'name', '1', 'name']) > 0

    Matchers.INSTANCE.matchesPath('$[*]', ['$', 'str']) == 0
  }

}
