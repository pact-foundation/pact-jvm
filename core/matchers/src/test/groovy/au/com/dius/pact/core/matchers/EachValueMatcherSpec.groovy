package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import spock.lang.Specification

class EachValueMatcherSpec extends Specification {

  private MatchingContext context

  def setup() {
    context = new MatchingContext(new MatchingRuleCategory('body'), true)
  }

  def 'matching json bodies - return no mismatches - with each like matcher on unequal lists'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('foo', TypeMatcher.INSTANCE, null, ''))
    def eachValueGroups = new EachValueMatcher(new MatchingRuleDefinition(
      '00000000000000000000000000000000',
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|\\*'),
      null, ''))
    context.matchers.addRule('$.resource_permissions', ValuesMatcher.INSTANCE)
    context.matchers.addRule('$.resource_permissions.*', TypeMatcher.INSTANCE)
    context.matchers.addRule('$.resource_permissions.*.resource.application_resource', TypeMatcher.INSTANCE)
    context.matchers.addRule('$.resource_permissions.*.resource.permissions', eachValue)
    context.matchers.addRule('$.resource_permissions.*.resource.groups', eachValueGroups)

    def actualBody = OptionalBody.body('''{
      "resource_permissions": {
        "a": {
          "resource": {
            "application_resource": "value 1",
            "permissions": ["a", "b", 100],
            "groups": ["*", "163ad478-10b7-11ee-9e1c-dbbb1ffc4ea4", "x"]
          },
          "effect": {
            "result": "ENFORCE_EFFECT_ALLOW"
          }
        }
      }
    }'''.bytes)
    def expectedBody = OptionalBody.body('''{
      "resource_permissions": {
        "permission": {
          "resource": {
            "application_resource": "foo",
            "permissions": ["foo"],
            "groups": ["*"]
          },
          "effect": {
            "result": "ENFORCE_EFFECT_ALLOW"
          }
        }
      }
    }'''.bytes)

    when:
    def result = JsonContentMatcher.INSTANCE.matchBody(expectedBody, actualBody, context)

    then:
    result.mismatches*.mismatch == [
      "Expected 100 (Integer) to be the same type as 'foo' (String)",
      "Expected 'x' to match '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|\\*'"
    ]
  }

  def 'matching json bodies - applies the rules defined below an each value matcher on a map to its values'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('{"main":{"amount":1000}}',
      TypeMatcher.INSTANCE, null, ''))
    context.matchers.addRule('$.chips', eachValue)
    context.matchers.addRule('$.chips.*', TypeMatcher.INSTANCE)
    context.matchers.addRule('$.chips.*.amount', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expectedBody = OptionalBody.body('{"chips": {"main": {"amount": 1000}}}'.bytes)
    def validBody = OptionalBody.body('{"chips": {"main": {"amount": 500}, "pair": {"amount": 100}}}'.bytes)
    def invalidBody = OptionalBody.body('{"chips": {"main": {"amount": 1.5}}}'.bytes)

    when:
    def validResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, validBody, context)

    then:
    validResult.mismatches.empty

    when:
    def invalidResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, invalidBody, context)

    then:
    invalidResult.mismatches*.path == ['$.chips.main.amount']
    invalidResult.mismatches*.mismatch == ['Expected 1.5 (Decimal) to be an integer']
  }

  def 'matching json bodies - applies the rules defined below an each value matcher on a list to its values'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('[{"amount":1000}]',
      TypeMatcher.INSTANCE, null, ''))
    context.matchers.addRule('$.items', eachValue)
    context.matchers.addRule('$.items[*].amount', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expectedBody = OptionalBody.body('{"items": [{"amount": 1000}]}'.bytes)
    def validBody = OptionalBody.body('{"items": [{"amount": 500}, {"amount": 100}]}'.bytes)
    def invalidBody = OptionalBody.body('{"items": [{"amount": 500}, {"amount": 1.5}]}'.bytes)

    when:
    def validResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, validBody, context)

    then:
    validResult.mismatches.empty

    when:
    def invalidResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, invalidBody, context)

    then:
    invalidResult.mismatches*.path == ['$.items[1].amount']
    invalidResult.mismatches*.mismatch == ['Expected 1.5 (Decimal) to be an integer']
  }

  def 'matching json bodies - applies the each value rules on a list when there are also rules for the list items'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('["abc"]', new RegexMatcher('[a-z]+'), null, ''))
    context.matchers.addRule('$.items', eachValue)
    context.matchers.addRule('$.items[*]', TypeMatcher.INSTANCE)
    def expectedBody = OptionalBody.body('{"items": ["abc"]}'.bytes)
    def validBody = OptionalBody.body('{"items": ["abc", "def"]}'.bytes)
    def invalidBody = OptionalBody.body('{"items": ["abc", "x1"]}'.bytes)

    when:
    def validResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, validBody, context)

    then:
    validResult.mismatches.empty

    when:
    def invalidResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, invalidBody, context)

    then:
    invalidResult.mismatches*.path == ['$.items[1]']
    invalidResult.mismatches*.mismatch == ["Expected 'x1' to match '[a-z]+'"]
  }

  def 'matching json bodies - does not combine the each value rules with rules for the values that use OR logic'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('{"a":"alice"}', new RegexMatcher('^[a-z]+$'),
      null, ''))
    context.matchers.addRule('$.names', eachValue)
    context.matchers.addRule('$.names.*', new RegexMatcher('^a.*'), RuleLogic.OR)
    context.matchers.addRule('$.names.*', new RegexMatcher('^b.*'), RuleLogic.OR)
    def expectedBody = OptionalBody.body('{"names": {"a": "alice"}}'.bytes)
    def validBody = OptionalBody.body('{"names": {"x": "bob", "y": "carol"}}'.bytes)
    def invalidBody = OptionalBody.body('{"names": {"x": "Bob1"}}'.bytes)

    when:
    def validResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, validBody, context)

    then:
    validResult.mismatches.empty

    when:
    def invalidResult = JsonContentMatcher.INSTANCE.matchBody(expectedBody, invalidBody, context)

    then:
    invalidResult.mismatches*.path == ['$.names.x']
    invalidResult.mismatches*.mismatch == ["Expected 'Bob1' to match '^[a-z]+\$'"]
  }

  def 'matching json bodies - does not apply rules for other paths to the values of an each value matcher'() {
    given:
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('{"main":{"amount":1000}}',
      TypeMatcher.INSTANCE, null, ''))
    context.matchers.addRule('$.chips', eachValue)
    context.matchers.addRule('$.other.amount', new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER))
    def expectedBody = OptionalBody.body('{"chips": {"main": {"amount": 1000}}, "other": {"amount": 1}}'.bytes)
    def actualBody = OptionalBody.body('{"chips": {"main": {"amount": 1.5}}, "other": {"amount": 1}}'.bytes)

    expect:
    JsonContentMatcher.INSTANCE.matchBody(expectedBody, actualBody, context).mismatches.empty
  }
}
