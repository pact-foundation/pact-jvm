package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.MatchingRuleGroup
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import spock.lang.Issue
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset

class DslSpec extends Specification {

  @Issue('#401')
  def 'eachKeyMappedToAnArrayLike does not work on "nested" property'() {
    given:
    def instant = OffsetDateTime.of(2000, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant()
    def body = new PactDslJsonBody()
      .datetime('date', "yyyyMMdd'T'HHmmss", instant, TimeZone.getTimeZone('GTM'))
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
      '$.date': new MatchingRuleGroup([new TimestampMatcher("yyyyMMdd'T'HHmmss")]),
      '$.system': new MatchingRuleGroup([new RegexMatcher('.+')]),
      '$.data': new MatchingRuleGroup([ValuesMatcher.INSTANCE]),
      '$.data.*[*].id': new MatchingRuleGroup([TypeMatcher.INSTANCE])
    ])
  }
}
