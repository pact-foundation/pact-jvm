package au.com.dius.pact.core.matchers.engine

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.HttpResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import spock.lang.Specification
import spock.lang.Unroll

class EachValueMatchingEngineSpec extends Specification {

  static final MatchingRule INTEGER = new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)

  def 'applies the rules defined below an each value matcher on a map to its values'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$.chips', eachValue('{"main":{"amount":1000}}', TypeMatcher.INSTANCE))
    body.addRule('$.chips.*.amount', INTEGER)
    def expected = '{"chips":{"main":{"amount":1000}}}'

    expect:
    mismatches(rules, expected, '{"chips":{"main":{"amount":500},"pair":{"amount":100}}}') == []
    mismatches(rules, expected, '{"chips":{"main":{"amount":500},"pair":{"amount":1.5}}}') ==
      ['$.chips.pair.amount: Expected 1.5 (Decimal) to be an integer']
  }

  def 'applies the rules defined below an each value matcher on a list to its values'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$.items', eachValue('[{"amount":1000}]', TypeMatcher.INSTANCE))
    body.addRule('$.items[*].amount', INTEGER)
    def expected = '{"items":[{"amount":1000}]}'

    expect:
    mismatches(rules, expected, '{"items":[{"amount":500},{"amount":100}]}') == []
    mismatches(rules, expected, '{"items":[{"amount":500},{"amount":1.5}]}') ==
      ['$.items[1].amount: Expected 1.5 (Decimal) to be an integer']
  }

  def 'applies the rules below an each value matcher at the root'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$', eachValue('{"main":{"amount":1000}}', TypeMatcher.INSTANCE))
    body.addRule('$.*.amount', INTEGER)

    expect:
    mismatches(rules, '{"main":{"amount":1000}}', '{"main":{"amount":500},"pair":{"amount":100}}') == []
    mismatches(rules, '{"main":{"amount":1000}}', '{"main":{"amount":500},"pair":{"amount":1.5}}') ==
      ['$.pair.amount: Expected 1.5 (Decimal) to be an integer']
  }

  def 'applies the rules below an each value matcher on a list at the root'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$', eachValue('[{"amount":1000}]', TypeMatcher.INSTANCE))
    body.addRule('$[*].amount', INTEGER)

    expect:
    mismatches(rules, '[{"amount":1000}]', '[{"amount":500},{"amount":100}]') == []
    mismatches(rules, '[{"amount":1000}]', '[{"amount":500},{"amount":1.5}]') ==
      ['$[1].amount: Expected 1.5 (Decimal) to be an integer']
  }

  def 'uses the map keys for the paths of the values, including keys that need to be quoted'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$.chips', eachValue('{"main":{"amount":1000}}', TypeMatcher.INSTANCE))
    body.addRule('$.chips.*.amount', INTEGER)

    expect:
    mismatches(rules, '{"chips":{"main":{"amount":1000}}}', '{"chips":{"big one":{"amount":1.5}}}') ==
      ["\$.chips['big one'].amount: Expected 1.5 (Decimal) to be an integer"]
  }

  def 'still applies the each value rules to map values that are not objects or lists'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule('$.names', eachValue('{"a":"alice"}', new RegexMatcher('^[a-z]+$')))

    expect:
    mismatches(rules, '{"names":{"a":"alice"}}', '{"names":{"x":"bob","y":"carol"}}') == []
    !mismatches(rules, '{"names":{"a":"alice"}}', '{"names":{"x":"Bob1"}}').empty
  }

  @Unroll
  def 'still applies the each value rules to the values of a #type when there are no rules below them'() {
    given:
    def rules = new MatchingRulesImpl()
    def body = rules.addCategory('body')
    body.addRule(path, eachValue(example, TypeMatcher.INSTANCE))

    expect:
    mismatches(rules, example, valid) == []
    !mismatches(rules, example, invalid).empty

    where:

    type   | path | example                    | valid                     | invalid
    'map'  | '$'  | '{"main":{"amount":1000}}' | '{"main":{"amount":1.5}}' | '{"main":"1000"}'
    'list' | '$'  | '[{"amount":1000}]'        | '[{"amount":1.5}]'        | '["1000"]'
  }

  private static EachValueMatcher eachValue(String example, MatchingRule rule) {
    new EachValueMatcher(new MatchingRuleDefinition(example, rule, null, ''))
  }

  private static List<String> mismatches(MatchingRulesImpl rules, String expected, String actual) {
    def json = ['Content-Type': ['application/json']]
    def expectedResponse = new HttpResponse(200, json, OptionalBody.body(expected.bytes, ContentType.JSON), rules)
    def actualResponse = new HttpResponse(200, json, OptionalBody.body(actual.bytes, ContentType.JSON))
    def pact = new V4Pact(new Consumer('consumer'), new Provider('provider'))
    def interaction = new V4Interaction.SynchronousHttp(null, 'test', [], new HttpRequest(), expectedResponse)
    def context = new PlanMatchingContext(pact, interaction, new MatchingConfiguration(false, false, true, false))
    def plan = V2MatchingEngine.INSTANCE.buildResponsePlan(expectedResponse, context)
    def result = V2MatchingEngine.INSTANCE.executeResponsePlan(plan, actualResponse, context).intoResponseMatchResult()
    result.body.bodyResults.collectMany { item -> item.result.collect { "${it.path}: ${it.mismatch}".toString() } }
  }
}
