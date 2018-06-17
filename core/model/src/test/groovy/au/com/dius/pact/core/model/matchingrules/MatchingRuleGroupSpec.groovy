package au.com.dius.pact.core.model.matchingrules

import spock.lang.Specification
import spock.lang.Unroll

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
}
