package au.com.dius.pact.model.matchingrules

import au.com.dius.pact.model.PactSpecVersion
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['LineLength', 'SpaceAroundMapEntryColon'])
class CategorySpec extends Specification {

  @Unroll
  def 'generate #spec format body matchers'() {
    given:
    def category = new Category('body', [
      '$[0]'      : new MatchingRuleGroup([new MaxTypeMatcher(5)]),
      '$[0][*].id': new MatchingRuleGroup([new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')])
    ])

    expect:
    category.toMap(spec) == matchers

    where:

    spec                 | matchers
    PactSpecVersion.V1   | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V1_1 | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V2   | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V3   | [
      '$[0]': [matchers: [[match: 'type', max: 5]], combine: 'AND'],
      '$[0][*].id': [matchers: [[match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']], combine: 'AND']]
  }

  @Issue('#743')
  def 'writes path matchers in the correct format'() {
    given:
    def category = new Category('path', [
      '': new MatchingRuleGroup([new RegexMatcher('\\w+')])
    ])

    expect:
    category.toMap(PactSpecVersion.V2) == ['$.path': [match: 'regex', regex: '\\w+']]
    category.toMap(PactSpecVersion.V3) == [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']
  }

  @Issue(['#786', '#882'])
  def 'writes header matchers in the correct format'() {
    given:
    def category = new Category('header', [
      'Content-Type': new MatchingRuleGroup([new RegexMatcher('application/json;\\s?charset=(utf|UTF)-8')])
    ])

    expect:
    category.toMap(PactSpecVersion.V2) == ['$.headers.Content-Type': [match: 'regex', regex: 'application/json;\\s?charset=(utf|UTF)-8']]
    category.toMap(PactSpecVersion.V3) == ['Content-Type': [matchers: [[match: 'regex', regex: 'application/json;\\s?charset=(utf|UTF)-8']], combine: 'AND']]
  }

  @Issue(['#895'])
  def 'when re-keying the matchers, drop any dollar from the start'() {
    given:
    def category = new Category('body', [
      '$.bestandstype': new MatchingRuleGroup([TypeMatcher.INSTANCE]),
      '$.bestandsid': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ])
    category.applyMatcherRootPrefix('payload')

    expect:
    category.toMap(PactSpecVersion.V2) == [
      '$.body.payload.bestandstype': [match: 'type'],
      '$.body.payload.bestandsid': [match: 'type']
    ]
    category.toMap(PactSpecVersion.V3) == [
      'payload.bestandstype': [matchers: [[match: 'type']], combine: 'AND'],
      'payload.bestandsid': [matchers: [[match: 'type']], combine: 'AND']
    ]
  }
  @Issue(['#976'])
  def 'when re-keying the matchers, always prepend prefix to existing key'() {
    given:
    def matchingRule = new MatchingRuleGroup([TypeMatcher.INSTANCE])
    def category = new Category('body', [
            '.blueberry': matchingRule
    ])
    category.applyMatcherRootPrefix('blue')

    expect:
    category.toMap(PactSpecVersion.V2) == [
            '$.body.blue.blueberry': [match: 'type']]

    category.toMap(PactSpecVersion.V3) == [
            'blue.blueberry': [matchers: [[match: 'type']], combine: 'AND']]
  }
}
