package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import kotlin.Triple
import org.apache.commons.lang3.time.DateFormatUtils
import spock.lang.Issue
import spock.lang.Specification

@SuppressWarnings('LineLength')
class ArrayContainsSpec extends Specification {
  @Issue('#1318')
  def 'array contains with simple values'() {
    given:
    def builder = new PactBodyBuilder(mimetype: 'application/json')

    when:
    builder {
      array arrayContaining([
        string('a'),
        numeric(100)
      ])
    }
    def rules = builder.matchers.matchingRules

    then:
    builder.body == '''{
    |    "array": [
    |        "a",
    |        100
    |    ]
    |}'''.stripMargin()
    rules.keySet() == ['$.array'] as Set
    rules['$.array'].rules.size() == 1
    rules['$.array'].rules[0] instanceof au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
    rules['$.array'].rules[0].variants == [
      new Triple(0, new MatchingRuleCategory('body', ['$': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE])]), [:]),
      new Triple(1, new MatchingRuleCategory('body', ['$': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)])]), [:])
    ]
  }

  @Issue('#1318')
  def 'array contains with simple values and generators'() {
    given:
    def builder = new PactBodyBuilder(mimetype: 'application/json')

    when:
    builder {
      array arrayContaining([
        date('yyyy-MM-dd'),
        equalTo('Test'),
        uuid()
      ])
    }
    def rules = builder.matchers.matchingRules
    def date = DateFormatUtils.ISO_DATE_FORMAT.format(new Date(Matchers.DATE_2000))

    then:
    builder.body == ('''{
    |    "array": [
    |        "''' + date + '''",
    |        "Test",
    |        "e2490de5-5bd3-43d5-b7c4-526e33f71304"
    |    ]
    |}''').stripMargin()
    rules.keySet() == ['$.array'] as Set
    rules['$.array'].rules.size() == 1
    rules['$.array'].rules[0] instanceof au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
    rules['$.array'].rules[0].variants.size() == 3
    rules['$.array'].rules[0].variants[0] == new Triple(
      0,
      new MatchingRuleCategory('body', ['$': new MatchingRuleGroup([new au.com.dius.pact.core.model.matchingrules.DateMatcher('yyyy-MM-dd')])]),
      ['$': new DateGenerator('yyyy-MM-dd')]
    )
    rules['$.array'].rules[0].variants[1] == new Triple(
      1,
      new MatchingRuleCategory('body', ['$': new MatchingRuleGroup([au.com.dius.pact.core.model.matchingrules.EqualsMatcher.INSTANCE])]),
      [:]
    )
    rules['$.array'].rules[0].variants[2] == new Triple(
      2,
      new MatchingRuleCategory('body', ['$': new MatchingRuleGroup([new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')])]),
      ['$': UuidGenerator.INSTANCE]
    )
  }
}
