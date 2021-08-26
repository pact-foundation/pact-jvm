package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import com.github.michaelbull.result.Err
import kotlin.Triple
import spock.lang.Specification

class MatchingDefinitionParserSpec extends Specification {
  def 'if the string does not start with matching'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression) instanceof Err

    where:

    expression << [
      '',
      'a, b, c',
      'matching some other text'
    ]
  }

  def 'parse type matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple('Name', TypeMatcher.INSTANCE, null)

    where:

    expression << [
      "matching(type,'Name')",
      "matching( type , 'Name' ) "
    ]
  }

  def 'parse number matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple(value, new NumberTypeMatcher(matcher), null)

    where:

    expression                   | value      | matcher
    'matching(number,100)'       | '100'      | NumberTypeMatcher.NumberType.NUMBER
    'matching(number, -100.101)' | '-100.101' | NumberTypeMatcher.NumberType.NUMBER
    'matching(integer,100)'      | '100'      | NumberTypeMatcher.NumberType.INTEGER
    'matching(decimal,100.101)'  | '100.101'  | NumberTypeMatcher.NumberType.DECIMAL
  }

  def 'invalid number matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression) instanceof Err

    where:

    expression << [
      'matching(integer,100.101)'
    ]
  }

  def 'parse datetime matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple(value, matcherClass.newInstance(format), null)

    where:

    expression                                                        | format                | value                 | matcherClass
    "matching(datetime, 'yyyy-MM-dd HH:mm:ss','2000-01-01 12:00:00')" | 'yyyy-MM-dd HH:mm:ss' | '2000-01-01 12:00:00' | TimestampMatcher
    "matching(date, 'yyyy-MM-dd','2000-01-01')"                       | 'yyyy-MM-dd'          | '2000-01-01'          | DateMatcher
    "matching(time, 'HH:mm:ss','12:00:00')"                           | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher
  }

  def 'parse regex matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple(value, new RegexMatcher(regex), null)

    where:

    expression                       | regex  | value
    "matching(regex, '\\w+','Fred')" | '\\w+' | 'Fred'
  }

  def 'parse include matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple(value, new IncludeMatcher(value), null)

    where:

    expression                          | value
    "matching(include, 'Fred and Bob')" | 'Fred and Bob'
  }

  def 'parse boolean matcher'() {
    expect:
    MatchingRuleDefinition.INSTANCE.parseMatchingRuleDefinition(expression).component1() ==
      new Triple(value, BooleanMatcher.INSTANCE, null)

    where:

    expression                | value
    "matching(boolean, true)" | 'true'
  }
}
