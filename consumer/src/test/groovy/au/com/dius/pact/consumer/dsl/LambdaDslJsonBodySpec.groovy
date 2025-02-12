package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import groovy.json.JsonSlurper
import kotlin.Triple
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody

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
    def root = newJsonBody { }
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

  @Issue('#1367')
  def 'array contains test'() {
    when:
    def result = newJsonBody(o -> {
      o.arrayContaining('output', a -> {
        a.stringValue('a')
      });
    }).build()

    then:
    result.body.toString() == '{"output":["a"]}'
    result.matchers.matchingRules == [
      '$.output': new MatchingRuleGroup([
        new ArrayContainsMatcher([new Triple(0, new MatchingRuleCategory('body'), [:])])
      ])
    ]
  }

  @Issue('#1367')
  def 'array contains test with two variants'() {
    when:
    def result = newJsonBody(o -> {
      o.arrayContaining('output', a -> {
        a.stringValue('a')
        a.numberValue(1)
      });
    }).build()

    then:
    result.body.toString() == '{"output":["a",1]}'
    result.matchers.matchingRules == [
      '$.output': new MatchingRuleGroup([
        new ArrayContainsMatcher([
          new Triple(0, new MatchingRuleCategory('body'), [:]),
          new Triple(1, new MatchingRuleCategory('body'), [:])
        ])
      ])
    ]
  }

  @Issue('#1850')
  def 'multiple example values'() {
    when:
    def oldDsl = new PactDslJsonBody()
      .minArrayLike('features', 1, 2)
      .stringType('name', 'FEATURE', 'FEATURE_2')
      .close()
    def newDsl = newJsonBody { o ->
      o.minArrayLike('features', 1, 2) { feature ->
        feature.stringType('name', 'FEATURE', 'FEATURE_2')
      }
    }.build()

    then:
    oldDsl.body.toString() == newDsl.body.toString()
  }

  @Issue('#1851')
  def 'body with keys with only digits'() {
    when:
    def result = newJsonBody(o -> {
      o.object('1234567890', o2 -> {
        o2.eachLike('name', a -> {
          a.stringType('@class', 'Test')
        })
      })
    }).build()

    then:
    result.body.toString() == '{"1234567890":{"name":[{"@class":"Test"}]}}'
    result.matchers.matchingRules == [
      "\$.1234567890.name": new MatchingRuleGroup([ TypeMatcher.INSTANCE ]),
      "\$.1234567890.name[*].@class": new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ]
  }
}
