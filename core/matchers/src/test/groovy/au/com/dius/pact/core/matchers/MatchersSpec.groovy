package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.InvalidPathExpression
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NullMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings('ClosureAsLastMethodParameter')
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

  def 'matchers defined - uses any provided path comparator'() {
    expect:
    Matchers.matcherDefined('header', ['SOMETHING'], matchingRules(),
      { String a, String b -> a.compareToIgnoreCase(b) } as Comparator<String>)

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('header').addRule('something', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be true when ignore-order matcher defined on path'() {
    expect:
    Matchers.isEqualsIgnoreOrderMatcherDefined(['$', 'array1'], 'body', matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.array1', new MinMaxEqualsIgnoreOrderMatcher(3, 5))
        .addRule('$.array1[*].foo', new RegexMatcher('a|b'))
        .addRule('$.array1[*].status', new RegexMatcher('up'))
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be true when ignore-order matcher defined on ancestor'() {
    expect:
    Matchers.isEqualsIgnoreOrderMatcherDefined(['$', 'any'], 'body', matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
              .addRule('$', EqualsIgnoreOrderMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be false when ignore-order matcher not defined on path'() {
    expect:
    !Matchers.isEqualsIgnoreOrderMatcherDefined(['$', 'array1', '0', 'foo'], 'body', matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.array1[*].foo', new RegexMatcher('a|b'))
        .addRule('$.array1[*].status', new RegexMatcher('up'))
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be false when ignore-order matcher not defined on ancestor'() {
    expect:
    !Matchers.isEqualsIgnoreOrderMatcherDefined(['$', 'any'], 'body', matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
              .addRule('$', new MinMaxTypeMatcher(3, 5))
      matchingRules
    }
  }

  def 'wildcardIndexMatcherDefined - should be false when the path does not have a matcher entry'() {
    expect:
    !Matchers.wildcardIndexMatcherDefined(['$', '0'], 'body', new MatchingRulesImpl())
  }

  def 'wildcardIndexMatcherDefined - should be false when the path has a non-wildcard matcher entry'() {
    expect:
    !Matchers.wildcardIndexMatcherDefined(['$', 'arr', '0'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.arr[0]', TypeMatcher.INSTANCE)
        .addRule('$.other[*]', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'wildcardIndexMatcherDefined - should be true when the path does have a matcher entry and it is a wildcard'() {
    expect:
    Matchers.wildcardIndexMatcherDefined(['$', '0'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$[*]', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'wildcardIndexMatcherDefined - should be false when a parent of the path has a matcher entry'() {
    expect:
    !Matchers.wildcardIndexMatcherDefined(['$', 'some', 'thing'], 'body' , matchingRules())

    where:
    matchingRules = {
      def matchingRules = new MatchingRulesImpl()
      matchingRules.addCategory('body')
        .addRule('$.*', TypeMatcher.INSTANCE)
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

  def 'wildcardMatchingEnabled - disabled by default'() {
    expect:
    !Matchers.wildcardMatchingEnabled()
  }

  @RestoreSystemProperties
  @Unroll
  def 'wildcardMatchingEnabled - #enabledOrDisabled when pact.matching.wildcard = "#value"'() {
    given:
    def testInvocation = { String v ->
      System.setProperty('pact.matching.wildcard', v)
      Matchers.wildcardMatchingEnabled()
    }

    expect:
    testInvocation(value) == enabled

    where:

    value       | enabledOrDisabled | enabled
    ''          | 'disabled'        | false
    '  '        | 'disabled'        | false
    'somevalue' | 'disabled'        | false
    'false'     | 'disabled'        | false
    ' false   ' | 'disabled'        | false
    'true'      | 'enabled'         | true
    '  true   ' | 'enabled'         | true

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
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', TypeMatcher.INSTANCE)
    def expected = OptionalBody.body('{"value": [100]}'.bytes)
    def actual = OptionalBody.body('{"value": ["200.3"]}'.bytes)

    when:
    def mismatches = new JsonBodyMatcher().matchBody(expected, actual, context).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected "200.3" (String) to be the same type as 100 (Integer)']
  }

  def 'type matcher - match on type - map elements should inherit the matchers from the parent'() {
    given:
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)
    context.matchers.addRule('$.value', TypeMatcher.INSTANCE)
    def expected = OptionalBody.body('{"value": {"a": 100}}'.bytes)
    def actual = OptionalBody.body('{"value": {"a": "200.3", "b": 200, "c": 300} }'.bytes)

    when:
    def mismatches = new JsonBodyMatcher().matchBody(expected, actual, context).mismatches

    then:
    !mismatches.empty
    mismatches*.mismatch == ['Expected "200.3" (String) to be the same type as 100 (Integer)']
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

  def 'path matching - throws an exception if path is invalid'() {
    when:
    Matchers.INSTANCE.matchesPath("\$.serviceNode.entity.status.thirdNode['@description]", ['a'])

    then:
    thrown(InvalidPathExpression)
  }

  def 'calculatePathWeight - throws an exception if path is invalid'() {
    when:
    Matchers.INSTANCE.calculatePathWeight("\$.serviceNode.entity.status.thirdNode['@description]", ['a'])

    then:
    thrown(InvalidPathExpression)
  }

  def 'resolveMatchers returns all matchers for the general case'() {
    given:
    def matchers = new MatchingRulesImpl()
    def status = matchers.addCategory('status')
      .addRule(EqualsMatcher.INSTANCE)
      .addRule(NullMatcher.INSTANCE)
    matchers.addCategory('body').addRule(new IncludeMatcher('A'))

    expect:
    Matchers.INSTANCE.resolveMatchers(matchers, 'status', [], { }) == status
  }

  def 'resolveMatchers returns matchers filtered by path length for body category'() {
    given:
    def matchers = new MatchingRulesImpl()
    matchers.addCategory('status')
      .addRule(EqualsMatcher.INSTANCE)
      .addRule(NullMatcher.INSTANCE)
    matchers.addCategory('body')
      .addRule('$.X', new IncludeMatcher('A'))
      .addRule('$.Y', EqualsMatcher.INSTANCE)
    def expected = new MatchingRuleCategory('body')
      .addRule('$.X', new IncludeMatcher('A'))

    expect:
    Matchers.INSTANCE.resolveMatchers(matchers, 'body', ['$', 'X'], { }) == expected
  }

  @Unroll
  def 'resolveMatchers returns matchers filtered by path for #category'() {
    given:
    def matchers = new MatchingRulesImpl()
    matchers.addCategory('status')
      .addRule(EqualsMatcher.INSTANCE)
    matchers.addCategory('body')
      .addRule('$.X', new IncludeMatcher('A'))
    matchers.addCategory('header')
      .addRule('X', new IncludeMatcher('X'))
      .addRule('Expected', new IncludeMatcher('Expected'))
    matchers.addCategory('query')
      .addRule('Q', new IncludeMatcher('Q'))
      .addRule('Expected', new IncludeMatcher('Expected'))
    matchers.addCategory('metadata')
      .addRule('M', new IncludeMatcher('M'))
      .addRule('Expected', new IncludeMatcher('Expected'))

    expect:
    Matchers.INSTANCE.resolveMatchers(matchers, category, ['Expected'],
      { a, b -> a <=> b }).matchingRules == [
        Expected: new MatchingRuleGroup([new IncludeMatcher('Expected')])
      ]

    where:

    category << [ 'header', 'query', 'metadata' ]
  }

}
