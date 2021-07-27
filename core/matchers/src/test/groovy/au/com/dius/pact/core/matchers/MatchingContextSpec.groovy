package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.InvalidPathExpression
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NullMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import kotlin.Triple
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import static au.com.dius.pact.core.support.json.JsonParser.parseString

@SuppressWarnings('ClosureAsLastMethodParameter')
class MatchingContextSpec extends Specification {

  def 'matchers defined - should be false when there are no matchers'() {
    expect:
    !new MatchingContext(new MatchingRuleCategory('body'), true).matcherDefined([''])
  }

  def 'matchers defined - should be false when the path does not have a matcher entry'() {
    expect:
    !new MatchingContext(new MatchingRuleCategory('body'), true).matcherDefined(['$', 'something'])
  }

  def 'matchers defined - should be true when the path does have a matcher entry'() {
    expect:
    new MatchingContext(matchingRules(), true).matcherDefined(['$', 'something'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules.addRule('$.something', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'matchers defined - should be true when a parent of the path has a matcher entry'() {
    expect:
    new MatchingContext(matchingRules(), true).matcherDefined(['$', 'something'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules.addRule('$', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'matchers defined - uses any provided path comparator'() {
    expect:
    new MatchingContext(matchingRules(), true).matcherDefined(['SOMETHING'],
      { String a, String b -> a.compareToIgnoreCase(b) } as Comparator<String>)

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('header')
      matchingRules.addRule('something', TypeMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be true when ignore-order matcher defined on path'() {
    expect:
    new MatchingContext(matchingRules(), true).isEqualsIgnoreOrderMatcherDefined(['$', 'array1'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules
        .addRule('$.array1', new MinMaxEqualsIgnoreOrderMatcher(3, 5))
        .addRule('$.array1[*].foo', new RegexMatcher('a|b'))
        .addRule('$.array1[*].status', new RegexMatcher('up'))
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be true when ignore-order matcher defined on ancestor'() {
    expect:
    new MatchingContext(matchingRules(), true).isEqualsIgnoreOrderMatcherDefined(['$', 'any'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules.addRule('$', EqualsIgnoreOrderMatcher.INSTANCE)
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be false when ignore-order matcher not defined on path'() {
    expect:
    !new MatchingContext(matchingRules(), true).isEqualsIgnoreOrderMatcherDefined(['$', 'array1', '0', 'foo'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules
        .addRule('$.array1[*].foo', new RegexMatcher('a|b'))
        .addRule('$.array1[*].status', new RegexMatcher('up'))
      matchingRules
    }
  }

  def 'ignore-orderMatcherDefined - should be false when ignore-order matcher not defined on ancestor'() {
    expect:
    !new MatchingContext(matchingRules(), true).isEqualsIgnoreOrderMatcherDefined(['$', 'any'])

    where:
    matchingRules = {
      def matchingRules = new MatchingRuleCategory('body')
      matchingRules.addRule('$', new MinMaxTypeMatcher(3, 5))
      matchingRules
    }
  }

  def 'should default to a matching defined at a parent level'() {
    given:
    def matchingRules = new MatchingRuleCategory('body')
    matchingRules.addRule('$', TypeMatcher.INSTANCE)
    def context = new MatchingContext(matchingRules, true)

    when:
    def rules = context.selectBestMatcher(['$', 'value'])

    then:
    rules.rules.first() == TypeMatcher.INSTANCE
  }

  def 'with matching rules with the same weighting, select the one of the same path length'() {
    given:
    def matchingRules = new MatchingRuleCategory('body')
    matchingRules
      .addRule('$.rawArray', TypeMatcher.INSTANCE)
      .addRule('$.rawArrayEqTo', TypeMatcher.INSTANCE)
      .addRule('$.rawArrayEqTo[*]', EqualsMatcher.INSTANCE)
      .addRule('$.regexpRawArray', TypeMatcher.INSTANCE)
      .addRule('$.regexpRawArray[*]', new RegexMatcher('.+'))
    def context = new MatchingContext(matchingRules, true)

    when:
    def rules = context.selectBestMatcher(['$', 'rawArrayEqTo', '1'])

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
    def matchingRules = new MatchingRuleCategory('status')
    matchingRules.addRule(EqualsMatcher.INSTANCE)
      .addRule(NullMatcher.INSTANCE)
    def context = new MatchingContext(matchingRules, true)

    expect:
    context.resolveMatchers([], { }) == matchingRules
  }

  def 'resolveMatchers returns matchers filtered by path length for body category'() {
    given:
    def matchingRules = new MatchingRuleCategory('body')
    matchingRules
      .addRule('$.X', new IncludeMatcher('A'))
      .addRule('$.Y', EqualsMatcher.INSTANCE)
    def expected = new MatchingRuleCategory('body')
      .addRule('$.X', new IncludeMatcher('A'))
    def context = new MatchingContext(matchingRules, true)

    expect:
    context.resolveMatchers(['$', 'X'], { }) == expected
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
    new MatchingContext(matchers.rules[category], true).resolveMatchers(['Expected'],
      { a, b -> a <=> b }).matchingRules == [
        Expected: new MatchingRuleGroup([new IncludeMatcher('Expected')])
      ]

    where:

    category << [ 'header', 'query', 'metadata' ]
  }

  @Issue('#1367')
  def 'array contains matcher with simple values'() {
    given:
    def matcher = new ArrayContainsMatcher([new Triple(0, new MatchingRuleCategory('body'), [:])])
    def expected = parseString('["a"]').asArray()
    def actual = parseString('["a", 1, {"id": 10,"name": "john"}]').asArray()
    def actual2 = parseString('["b", 1, {"id": 10,"name": "john"}]').asArray()
    def mismatchFactory = [
      create: { e, a, des, p -> new BodyMismatch(e.toString(), a.toString(), des) }
    ] as MismatchFactory
    def callback = { path, e, a, context ->
      def result = MatcherExecutorKt.matchEquality(path, e, a, mismatchFactory)
      [ new BodyItemMatchResult('0', result) ]
    }
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)

    expect:
    Matchers.INSTANCE.compareLists(['$'], matcher, expected.values, actual.values, context,
      { -> }, callback).empty
    !Matchers.INSTANCE.compareLists(['$'], matcher, expected.values, actual2.values, context,
      { -> }, callback).empty
  }

  @Issue('#1367')
  def 'array contains matcher with two expected values'() {
    given:
    def matcher = new ArrayContainsMatcher([
      new Triple(0, new MatchingRuleCategory('body'), [:]),
      new Triple(1, new MatchingRuleCategory('body'), [:])
    ])
    def expected = parseString('["a", 1]').asArray()
    def actual = parseString('["a", {"id": 10,"name": "john"}, 1, false]').asArray()
    def actual2 = parseString('["b", {"id": 10,"name": "john"}, 1, true]').asArray()
    def actual3 = parseString('["b", {"id": 10,"name": "john"}, 5, true]').asArray()
    def mismatchFactory = [
      create: { e, a, des, p -> new BodyMismatch(e.toString(), a.toString(), des) }
    ] as MismatchFactory
    def callback = { path, e, a, context ->
      def result = MatcherExecutorKt.matchEquality(path, e, a, mismatchFactory)
      [ new BodyItemMatchResult('0', result) ]
    }
    def context = new MatchingContext(new MatchingRuleCategory('body'), true)

    when:
    def result1 = Matchers.INSTANCE.compareLists(['$'], matcher, expected.values, actual.values,
      context, { -> }, callback)
    def result2 = Matchers.INSTANCE.compareLists(['$'], matcher, expected.values, actual2.values,
      context, { -> }, callback)
    def result3 = Matchers.INSTANCE.compareLists(['$'], matcher, expected.values, actual3.values,
      context, { -> }, callback)

    then:
    result1.empty
    result2.size() == 1
    result3.size() == 2
  }
}
