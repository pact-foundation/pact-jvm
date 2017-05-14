package au.com.dius.pact.model.matchingrules

import au.com.dius.pact.model.PactSpecVersion
import spock.lang.Specification
import spock.lang.Unroll

class CategorySpec extends Specification {

  def 'defaults to AND for combining rules'() {
    expect:
    new Category().ruleLogic == RuleLogic.AND
  }

  @Unroll
  @SuppressWarnings(['LineLength', 'SpaceAroundMapEntryColon'])
  def 'generate #spec format body matchers'() {
    given:
    def category = new Category(name: 'body', matchingRules: [
      '$[0]'      : [new MaxTypeMatcher(5)],
      '$[0][*].id': [new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')]
    ])

    expect:
    category.toMap(spec) == matchers

    where:

    spec                 | matchers
    PactSpecVersion.V1   | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V1_1 | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V2   | ['$.body[0]': [match: 'type', max: 5], '$.body[0][*].id': [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]
    PactSpecVersion.V3   | ['$[0]': [matchers: [[match: 'type', max: 5]]], '$[0][*].id': [matchers: [[match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']]]]
  }
}
