package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Either
import au.com.dius.pact.core.support.Result
import spock.lang.Specification

@SuppressWarnings(['LineLength', 'UnnecessaryGString'])
class MatchingDefinitionParserSpec extends Specification {
  def 'if the string does not start with a valid matching definition'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression) instanceof Result.Err

    where:

    expression << [
      '',
      'a, b, c',
      'matching some other text'
    ]
  }

  def 'parse type matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition('Name', TypeMatcher.INSTANCE, null)

    where:

    expression << [
      "matching(type,'Name')",
      "matching( type , 'Name' ) "
    ]
  }

  def 'parse number matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, new NumberTypeMatcher(matcher), null)

    where:

    expression                   | value      | matcher
    'matching(number,100)'       | '100'      | NumberTypeMatcher.NumberType.NUMBER
    'matching( number , 100 )'   | '100'      | NumberTypeMatcher.NumberType.NUMBER
    'matching(number, -100.101)' | '-100.101' | NumberTypeMatcher.NumberType.NUMBER
    'matching(integer,100)'      | '100'      | NumberTypeMatcher.NumberType.INTEGER
    'matching(decimal,100.101)'  | '100.101'  | NumberTypeMatcher.NumberType.DECIMAL
  }

  def 'invalid number matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression) instanceof Result.Err

    where:

    expression << [
      'matching(integer,100.101)'
    ]
  }

  def 'parse datetime matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, matcherClass.newInstance(format), null)

    where:

    expression                                                        | format                | value                 | matcherClass
    "matching(datetime, 'yyyy-MM-dd HH:mm:ss','2000-01-01 12:00:00')" | 'yyyy-MM-dd HH:mm:ss' | '2000-01-01 12:00:00' | TimestampMatcher
    "matching(date, 'yyyy-MM-dd','2000-01-01')"                       | 'yyyy-MM-dd'          | '2000-01-01'          | DateMatcher
    "matching(time, 'HH:mm:ss','12:00:00')"                           | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher
    "matching( time , 'HH:mm:ss' , '12:00:00' )"                      | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher
  }

  def 'parse regex matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, new RegexMatcher(regex), null)

    where:

    expression                            | regex  | value
    "matching(regex, '\\w+','Fred')"      | '\\w+' | 'Fred'
    "matching( regex , '\\w+' , 'Fred' )" | '\\w+' | 'Fred'
  }

  def 'invalid regex matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression) instanceof Result.Err

    where:

    expression << [ "matching(regex, null, 'Fred')" ]
  }

  def 'parse include matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, new IncludeMatcher(value), null)

    where:

    expression                             | value
    "matching(include, 'Fred and Bob')"    | 'Fred and Bob'
    "matching( include , 'Fred and Bob' )" | 'Fred and Bob'
  }

  def 'parse boolean matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, BooleanMatcher.INSTANCE, null)

    where:

    expression                    | value
    'matching(boolean, true)'     | 'true'
    'matching( boolean , false )' | 'false'
  }

  def 'each key and value'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(null, ValueType.Unknown, value, null)

    where:

    expression                                                       | value
    "eachKey(matching(regex, '\$(\\.\\w+)+', '\$.test.one'))"        | [Either.a(new EachKeyMatcher(new MatchingRuleDefinition('$.test.one', new RegexMatcher('$(\\.\\w+)+'), null)))]
    "eachKey(notEmpty('\$.test')), eachValue(matching(number, 100))" | [Either.a(new EachKeyMatcher(new MatchingRuleDefinition('$.test', ValueType.String, [ Either.a(NotEmptyMatcher.INSTANCE) ], null))), Either.a(new EachValueMatcher(new MatchingRuleDefinition('100', new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER), null)))]
    "eachValue(matching(\$'items'))"                                 | [Either.a(new EachValueMatcher(new MatchingRuleDefinition(null, ValueType.Unknown, [Either.b(new MatchingReference('items'))], null)))]
  }

  def 'invalid each key and value'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression) instanceof Result.Err

    where:

    expression << [
      "eachKey(regex, '\$(\\.\\w+)+', '\$.test.one')",
      'eachValue(number, 10)'
    ]
  }

  def 'parse notEmpty matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, type, [ Either.a(NotEmptyMatcher.INSTANCE) ], null)

    where:

    expression           | value  | type
    "notEmpty('true')"   | 'true' | ValueType.String
    "notEmpty( 'true' )" | 'true' | ValueType.String
    'notEmpty(true)'     | 'true' | ValueType.Boolean
  }

  def 'parsing string values'() {
    expect:
    new MatcherDefinitionParser(new MatcherDefinitionLexer(expression)).string().value == result

    where:

    expression                                      | result
    "''"                                            | ''
    "'Example value'"                               | 'Example value'
    "'yyyy-MM-dd HH:mm:ssZZZZZ'"                    | 'yyyy-MM-dd HH:mm:ssZZZZZ'
    "'2020-05-21 16:44:32+10:00'"                   | '2020-05-21 16:44:32+10:00'
    "'\\w{3}\\d+'"                                  | "\\w{3}\\d+"
    "'<?xml?><test/>'"                              | '<?xml?><test/>'
    "'\\\$(\\.\\w+)+'"                              | "\\\$(\\.\\w+)+"
    "'we don\\'t currently support parallelograms'" | "we don\\'t currently support parallelograms"
    "'\\b backspace'"                               | "\b backspace"
    "'\\f formfeed'"                                | "\f formfeed"
    "'\\n linefeed'"                                | "\n linefeed"
    "'\\r carriage return'"                         | "\r carriage return"
    "'\\t tab'"                                     | "\t tab"
    "'\\u0109 unicode hex code'"                    | "\u0109 unicode hex code"
    "'\\u{1DF0B} unicode hex code'"                 | "${Character.toString(0x1DF0B)} unicode hex code"
    "'\\u{1D400} unicode hex code'"                 | "𝐀 unicode hex code"
  }

  def 'process raw string'() {
    expect:
    new MatcherDefinitionParser(new MatcherDefinitionLexer("")).processRawString(expression).value == result

    where:

    expression                  | result
    ''                          | ""
    'Example value'             | 'Example value'
    'not escaped \\$(\\.\\w+)+' | 'not escaped \\$(\\.\\w+)+'
    'escaped \\\\'              | 'escaped \\'
    'slash at end \\'           | 'slash at end \\'
  }

  def "process raw string error test"() {
    given:
    def parser = new MatcherDefinitionParser(new MatcherDefinitionLexer("'invalid escape \\u in string'"))

    expect:
    parser.processRawString("'invalid escape \\u in string'").errorValue() == "Invalid unicode escape found at index 0"
    parser.processRawString('\\u0') instanceof Result.Err
    parser.processRawString('\\u00') instanceof Result.Err
    parser.processRawString('\\u000') instanceof Result.Err
    parser.processRawString('\\u{000') instanceof Result.Err
  }
}
