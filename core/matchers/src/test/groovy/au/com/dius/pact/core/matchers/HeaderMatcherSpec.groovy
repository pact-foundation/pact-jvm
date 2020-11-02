package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.RuleLogic
import spock.lang.Specification
import spock.lang.Unroll

class HeaderMatcherSpec extends Specification {

  private MatchingContext context

  def setup() {
    context = new MatchingContext(new MatchingRuleCategory('header'), true)
  }

  def "matching headers - be true when headers are equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADER', context) == null
  }

  def "matching headers - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'HEADSER', context) != null
  }

  def "matching headers - exclude whitespace from the comparison"() {
    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER1, HEADER2,   3', 'HEADER1,HEADER2,3',
      context) == null
  }

  def "matching headers - delegate to a matcher when one is defined"() {
    given:
    context.matchers.addRule('HEADER', new RegexMatcher('.*'))

    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'XYZ', context) == null
  }

  def "matching headers - combines mismatches if there are multiple"() {
    given:
    context.matchers.addRule('HEADER', new RegexMatcher('X=.*'), RuleLogic.OR)
    context.matchers.addRule('HEADER', new RegexMatcher('A=.*'), RuleLogic.OR)
    context.matchers.addRule('HEADER', new RegexMatcher('B=.*'), RuleLogic.OR)

    expect:
    HeaderMatcher.compareHeader('HEADER', 'HEADER', 'XYZ', context).mismatch ==
      "Expected 'XYZ' to match 'X=.*', Expected 'XYZ' to match 'A=.*', Expected 'XYZ' to match 'B=.*'"
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def "matching headers - content type header - be true when #description"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', expected, actual, context) == null

    where:

    description                                       | expected                         | actual
    'headers are equal'                               | 'application/json;charset=UTF-8' | 'application/json; charset=UTF-8'
    'headers are equal but have different case'       | 'application/json;charset=UTF-8' | 'application/JSON; charset=utf-8'
    'the charset is missing from the expected header' | 'application/json'               | 'application/json ; charset=utf-8'
  }

  def "matching headers - content type header - be false when headers are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/pdf;charset=UTF-8', context) != null
  }

  def "matching headers - content type header - be false when charsets are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;charset=UTF-8',
      'application/json;charset=UTF-16', context) != null
  }

  def "matching headers - content type header - be false when other parameters are not equal"() {
    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json;declaration="<950118.AEB0@XIson.com>"',
      'application/json;charset=UTF-8', context) != null
  }

  def "matching headers - content type header - delegate to any defined matcher"() {
    given:
    context.matchers.addRule('CONTENT-TYPE', new RegexMatcher('[a-z]+\\/[a-z]+'))

    expect:
    HeaderMatcher.compareHeader('CONTENT-TYPE', 'application/json',
      'application/json;charset=UTF-8', context) != null
    HeaderMatcher.compareHeader('content-type', 'application/json',
      'application/json;charset=UTF-8', context) != null
    HeaderMatcher.compareHeader('Content-Type', 'application/json',
      'application/json;charset=UTF-8', context) != null
  }

  def "parse parameters - parse the parameters into a map"() {
    expect:
    HeaderMatcher.parseParameters(['A=B']) == [A: 'B']
    HeaderMatcher.parseParameters(['A=B', 'C=D']) == [A: 'B', C: 'D']
    HeaderMatcher.parseParameters(['A= B', 'C =D ']) == [A: 'B', C: 'D']
  }

  @Unroll
  def 'strip whitespace test'() {
    expect:
    HeaderMatcher.INSTANCE.stripWhiteSpaceAfterCommas(str) == expected

    where:

    str         | expected
    ''          | ''
    ' '         | ' '
    'abc'       | 'abc'
    'abc xyz'   | 'abc xyz'
    'abc,xyz'   | 'abc,xyz'
    'abc, xyz'  | 'abc,xyz'
    'abc , xyz' | 'abc ,xyz'
  }
}
