package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class DslSpec extends Specification {

  @Unroll
  def 'correctly generates a key for an attribute name'() {
    expect:
    Dsl.matcherKey(name, 'a.b.c.') == result

    where:

    name         | result
    'a'          | 'a.b.c.a'
    'a1'         | 'a.b.c.a1'
    '_a'         | 'a.b.c._a'
    '@a'         | 'a.b.c.@a'
    '#a'         | 'a.b.c.#a'
    'b-a'        | 'a.b.c.b-a'
    'b:a'        | 'a.b.c.b:a'
    '01/01/2001' | "a.b.c['01/01/2001']"
    'a['         | "a.b.c['a[']"
  }

  @Issue('#401')
  def 'eachKeyMappedToAnArrayLike does not work on "nested" property'() {
    given:
    def body = new PactDslJsonBody()
      .date('date', "yyyyMMdd'T'HHmmss")
      .stringMatcher('system', '.+', 'systemname')
      .object('data')
        .eachKeyMappedToAnArrayLike('subsystem_name')
          .stringType('id', '1234567')
        .closeArray()
      .closeObject()

    when:
    def result = body.close()

    then:
    result.body.toString() ==
      '{"data":{"subsystem_name":[{"id":"1234567"}]},"date":"20000201T000000","system":"systemname"}'
    result.matchers == new MatchingRuleCategory('body', [
      '$.date': new MatchingRuleGroup([new DateMatcher("yyyyMMdd'T'HHmmss")]),
      '$.system': new MatchingRuleGroup([new RegexMatcher('.+')]),
      '$.data': new MatchingRuleGroup([ValuesMatcher.INSTANCE]),
      '$.data.*[*].id': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ])
  }
}
