package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Json
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class MatchingRuleGroupSpec extends Specification {

  @Unroll
  def 'from JSON'() {
    expect:
    MatchingRuleGroup.fromJson(Json.toJson(json)) == value

    where:

    json                                              | value
    [:]                                               | new MatchingRuleGroup()
    [other: 'value']                                  | new MatchingRuleGroup()
    [matchers: [[match: 'equality']]]                 | new MatchingRuleGroup([EqualsMatcher.INSTANCE])
    [matchers: [[match: 'equality']], combine: 'AND'] | new MatchingRuleGroup([EqualsMatcher.INSTANCE])
    [matchers: [[match: 'equality']], combine: 'OR']  | new MatchingRuleGroup([EqualsMatcher.INSTANCE], RuleLogic.OR)
    [matchers: [[match: 'equality']], combine: 'BAD'] | new MatchingRuleGroup([EqualsMatcher.INSTANCE])
  }

  def 'defaults to AND for combining rules'() {
    expect:
    new MatchingRuleGroup().ruleLogic == RuleLogic.AND
  }

  @Unroll
  def 'Converts number matchers to type matchers when spec is < V3'() {
    expect:
    new MatchingRuleGroup([matcher]).toMap(PactSpecVersion.V2) == map

    where:
    matcher                                                     | map
    EqualsMatcher.INSTANCE                                      | [match: 'equality']
    new RegexMatcher('.*')                                      | [match: 'regex', regex: '.*']
    TypeMatcher.INSTANCE                                        | [match: 'type']
    new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL) | [match: 'type']
    new MinTypeMatcher(1)                                       | [match: 'type', min: 1]
    new MaxTypeMatcher(1)                                       | [match: 'type', max: 1]
    new MinMaxTypeMatcher(2, 3)                                 | [match: 'type', max: 3, min: 2]
    new TimestampMatcher()                                      | [match: 'timestamp', timestamp: 'yyyy-MM-dd HH:mm:ssZZZZZ']
    new TimeMatcher()                                           | [match: 'time', time: 'HH:mm:ss']
    new DateMatcher()                                           | [match: 'date', date: 'yyyy-MM-dd']
    new IncludeMatcher('A')                                     | [match: 'include', value: 'A']
    ValuesMatcher.INSTANCE                                      | [match: 'values']
    NullMatcher.INSTANCE                                        | [match: 'null']
  }
}
