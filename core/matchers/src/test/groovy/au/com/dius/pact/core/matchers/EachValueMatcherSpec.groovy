package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
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
    def eachValue = new EachValueMatcher(new MatchingRuleDefinition('foo', TypeMatcher.INSTANCE, null))
    def eachValueGroups = new EachValueMatcher(new MatchingRuleDefinition(
      '00000000000000000000000000000000',
      new RegexMatcher('[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|\\*'),
      null))
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
}
