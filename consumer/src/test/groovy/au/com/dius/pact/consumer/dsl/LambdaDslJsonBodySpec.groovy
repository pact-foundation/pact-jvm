package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import groovy.json.JsonSlurper
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class LambdaDslJsonBodySpec extends Specification {

  @Issue('#1107')
  def 'handle datetimes with Zone IDs'() {
    given:
    def body = new LambdaDslJsonBody(new PactDslJsonBody())

    when:
    body.datetime('test', "yyyy-MM-dd'T'HH:mmx'['VV']'")
    def result = new JsonSlurper().parseText(body.pactDslObject.toString())

    then:
    result.test ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}[-+]\d+\[\w+(\/\w+)?]/
  }

  @Unroll
  def 'generates an array with ignore-order #expectedMatcher.class.simpleName matching'() {
    given:
    def root = LambdaDsl.newJsonBody { }
    root."$method"('a', *params) {
      it.stringValue('a')
        .stringType('b')
    }

    when:
    def result = root.build().close()

    then:
    result.body.toString() == '{"a":["a","b"]}'
    result.matchers.matchingRules == [
      '$.a': new MatchingRuleGroup([expectedMatcher]),
      '$.a[1]': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ]

    where:

    method                 | params | expectedMatcher
    'unorderedArray'       | []     | EqualsIgnoreOrderMatcher.INSTANCE
    'unorderedMinArray'    | [2]    | new MinEqualsIgnoreOrderMatcher(2)
    'unorderedMaxArray'    | [4]    | new MaxEqualsIgnoreOrderMatcher(4)
    'unorderedMinMaxArray' | [2, 4] | new MinMaxEqualsIgnoreOrderMatcher(2, 4)
  }

}
