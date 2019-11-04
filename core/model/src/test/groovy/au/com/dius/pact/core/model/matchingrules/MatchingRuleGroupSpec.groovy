package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class MatchingRuleGroupSpec extends Specification {

  @Unroll
  def 'matchers lookup returns #matcherClass.simpleName #condition'() {
    expect:
    MatchingRuleGroup.ruleFromMap(map).class == matcherClass

    where:
    map                              | matcherClass      | condition
    [:]                              | EqualsMatcher     | 'if the definition is empty'
    [other: 'value']                 | EqualsMatcher     | 'if the definition is invalid'
    [match: 'something']             | EqualsMatcher     | 'if the matcher type is unknown'
    [match: 'equality']              | EqualsMatcher     | 'if the matcher type is equality'
    [match: 'regex', regex: '.*']    | RegexMatcher      | 'if the matcher type is regex'
    [regex: '\\w+']                  | RegexMatcher      | 'if the matcher definition contains a regex'
    [match: 'type']                  | TypeMatcher       | 'if the matcher type is \'type\' and there is no min or max'
    [match: 'number']                | NumberTypeMatcher | 'if the matcher type is \'number\''
    [match: 'integer']               | NumberTypeMatcher | 'if the matcher type is \'integer\''
    [match: 'real']                  | NumberTypeMatcher | 'if the matcher type is \'real\''
    [match: 'decimal']               | NumberTypeMatcher | 'if the matcher type is \'decimal\''
    [match: 'type', min: 1]          | MinTypeMatcher    | 'if the matcher type is \'type\' and there is a min'
    [match: 'min', min: 1]           | MinTypeMatcher    | 'if the matcher type is \'min\''
    [min: 1]                         | MinTypeMatcher    | 'if the matcher definition contains a min'
    [match: 'type', max: 1]          | MaxTypeMatcher    | 'if the matcher type is \'type\' and there is a max'
    [match: 'max', max: 1]           | MaxTypeMatcher    | 'if the matcher type is \'max\''
    [max: 1]                         | MaxTypeMatcher    | 'if the matcher definition contains a max'
    [match: 'type', max: 3, min: 2]  | MinMaxTypeMatcher | 'if the matcher definition contains both a min and max'
    [match: 'timestamp']             | TimestampMatcher  | 'if the matcher type is \'timestamp\''
    [timestamp: '1']                 | TimestampMatcher  | 'if the matcher definition contains a timestamp'
    [match: 'time']                  | TimeMatcher       | 'if the matcher type is \'time\''
    [time: '1']                      | TimeMatcher       | 'if the matcher definition contains a time'
    [match: 'date']                  | DateMatcher       | 'if the matcher type is \'date\''
    [date: '1']                      | DateMatcher       | 'if the matcher definition contains a date'
    [match: 'include', include: 'A'] | IncludeMatcher    | 'if the matcher type is include'
    [match: 'values']                | ValuesMatcher     | 'if the matcher type is values'
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
    new TimestampMatcher()                                      | [match: 'timestamp', timestamp: 'yyyy-MM-dd HH:mm:ssZZZ']
    new TimeMatcher()                                           | [match: 'time', time: 'HH:mm:ss']
    new DateMatcher()                                           | [match: 'date', date: 'yyyy-MM-dd']
    new IncludeMatcher('A')                                     | [match: 'include', value: 'A']
    ValuesMatcher.INSTANCE                                      | [match: 'values']
    NullMatcher.INSTANCE                                        | [match: 'null']
  }
}
