package au.com.dius.pact.core.model.matchingrules.expressions

import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.Either
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.expressions.DataType
import spock.lang.Specification

@SuppressWarnings(['LineLength', 'UnnecessaryGString'])
class MatchingDefinitionParserSpec extends Specification {
  def 'if the string does not start with a valid matching definition'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).errorValue() == message

    where:

    expression                 | message
    ''                         | 'Error parsing expression: expression is empty'
    'a, b, c'                  | 'Error parsing expression: Was expecting a matching rule definition type at index 0\n        a, b, c\n        ^'
    'matching some other text' | 'Error parsing expression: Was expecting a \'(\' at index 9\n        matching some other text\n                 ^'
  }

  def 'parse type matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition('Name', TypeMatcher.INSTANCE, generator, expression.trim())

    where:

    expression                                         | generator
    "matching(type,'Name')"                            | null
    "matching( type , 'Name' ) "                       | null
    "matching(type, fromProviderState('exp', 'Name'))" | new ProviderStateGenerator('exp', DataType.STRING)
  }

  def 'parse equal to matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, EqualsMatcher.INSTANCE, generator, expression.trim())

    where:

    expression                                       | value   | generator
    "matching(equalTo,'Name')"                       | 'Name'  | null
    "matching( equalTo , 123.4 ) "                   | '123.4' | null
    "matching(equalTo, fromProviderState('exp', 3))" | '3'     | new ProviderStateGenerator('exp', DataType.INTEGER)
  }

  def 'parse number matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, new NumberTypeMatcher(matcher), generator, expression.trim())

    where:

    expression                                       | value      | matcher                              | generator
    'matching(number,100)'                           | '100'      | NumberTypeMatcher.NumberType.NUMBER  | null
    'matching( number , 100 )'                       | '100'      | NumberTypeMatcher.NumberType.NUMBER  | null
    'matching(number, -100.101)'                     | '-100.101' | NumberTypeMatcher.NumberType.NUMBER  | null
    'matching(integer,100)'                          | '100'      | NumberTypeMatcher.NumberType.INTEGER | null
    'matching(decimal,100.101)'                      | '100.101'  | NumberTypeMatcher.NumberType.DECIMAL | null
    "matching(number, fromProviderState('exp', 3))"  | '3'        | NumberTypeMatcher.NumberType.NUMBER  | new ProviderStateGenerator('exp', DataType.INTEGER)
    "matching(integer, fromProviderState('exp', 3))" | '3'        | NumberTypeMatcher.NumberType.INTEGER | new ProviderStateGenerator('exp', DataType.INTEGER)
    "matching(decimal, fromProviderState('exp', 3))" | '3'        | NumberTypeMatcher.NumberType.DECIMAL | new ProviderStateGenerator('exp', DataType.INTEGER)
  }

  def 'invalid number matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).errorValue() == error

    where:

    expression | error
    'matching(integer,100.101)' | 'Error parsing expression: Was expecting a \')\' at index 20\n        matching(integer,100.101)\n                            ^'
  }

  def 'parse datetime matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, matcherClass.newInstance(format), generator, expression.trim())

    where:

    expression                                                                                   | format                | value                 | matcherClass     | generator
    "matching(datetime, 'yyyy-MM-dd HH:mm:ss','2000-01-01 12:00:00')"                            | 'yyyy-MM-dd HH:mm:ss' | '2000-01-01 12:00:00' | TimestampMatcher | null
    "matching(date, 'yyyy-MM-dd','2000-01-01')"                                                  | 'yyyy-MM-dd'          | '2000-01-01'          | DateMatcher      | null
    "matching(time, 'HH:mm:ss','12:00:00')"                                                      | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher      | null
    "matching( time , 'HH:mm:ss' , '12:00:00' )"                                                 | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher      | null
    "matching(datetime, 'yyyy-MM-dd HH:mm:ss', fromProviderState('exp', '2000-01-01 12:00:00'))" | 'yyyy-MM-dd HH:mm:ss' | '2000-01-01 12:00:00' | TimestampMatcher | new ProviderStateGenerator('exp', DataType.STRING)
    "matching(date, 'yyyy-MM-dd', fromProviderState('exp', '2000-01-01'))"                       | 'yyyy-MM-dd'          | '2000-01-01'          | DateMatcher      | new ProviderStateGenerator('exp', DataType.STRING)
    "matching(time, 'HH:mm:ss', fromProviderState('exp', '12:00:00'))"                           | 'HH:mm:ss'            | '12:00:00'            | TimeMatcher      | new ProviderStateGenerator('exp', DataType.STRING)
  }

  def 'parse regex matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, new RegexMatcher(regex), null, expression.trim())

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
      new MatchingRuleDefinition(value, new IncludeMatcher(value), null, expression.trim())

    where:

    expression                             | value
    "matching(include, 'Fred and Bob')"    | 'Fred and Bob'
    "matching( include , 'Fred and Bob' )" | 'Fred and Bob'
  }

  def 'parse boolean matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition(value, BooleanMatcher.INSTANCE, null, expression.trim())

    where:

    expression                    | value
    'matching(boolean, true)'     | 'true'
    'matching( boolean , false )' | 'false'
  }

  def 'each key'() {
    given:
    def expression = "eachKey(matching(regex, '\$(\\.\\w+)+', '\$.test.one'))"
    def value = [
      Either.a(new EachKeyMatcher(
        new MatchingRuleDefinition('$.test.one',
          new RegexMatcher('$(\\.\\w+)+'), null, 'matching(regex, \'$(\\.\\w+)+\', \'$.test.one\')'))
      )
    ]

    when:
    def result = MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value

    then:
    result == new MatchingRuleDefinition(null, ValueType.Unknown, value, null, expression)
  }

  def 'each value with reference'() {
    given:
    def expression = "eachValue(matching(\$'items'))"
    def value = [
      Either.a(
        new EachValueMatcher(
          new MatchingRuleDefinition(
            null,
            ValueType.Unknown,
            [ Either.b(new MatchingReference('items')) ],
            null,
            "matching(\$'items')"
          )
        )
      )
    ]

    when:
    def result = MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value

    then:
    result.value == null
    result.valueType == ValueType.Unknown
    result.rules == value
    result.generator == null
    result.expression == expression
  }

  def 'each key and value'() {
    def expression = "eachKey(notEmpty('\$.test')), eachValue(matching(number, 100))"
    def value = [
      Either.a(new EachKeyMatcher(new MatchingRuleDefinition('$.test', ValueType.String, [ Either.a(NotEmptyMatcher.INSTANCE) ], null, "notEmpty('\$.test')"))),
      Either.a(new EachValueMatcher(new MatchingRuleDefinition('100', new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER), null, 'matching(number, 100)')))
    ]

    when:
    def result = MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value

    then:
    result == new MatchingRuleDefinition(null, ValueType.Unknown, value, null, expression)
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
      new MatchingRuleDefinition(value, type, [ Either.a(NotEmptyMatcher.INSTANCE) ], generator, expression.trim())

    where:

    expression                              | value  | type              | generator
    "notEmpty('true')"                      | 'true' | ValueType.String  | null
    "notEmpty( 'true' )"                    | 'true' | ValueType.String  | null
    'notEmpty(true)'                        | 'true' | ValueType.Boolean | null
    "notEmpty(fromProviderState('exp', 3))" | '3'    | ValueType.Integer | new ProviderStateGenerator('exp', DataType.INTEGER)
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

  def 'parse atLeast matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition("", ValueType.Unknown, [ Either.a(new MinTypeMatcher(value)) ], null, expression.trim())

    where:

    expression      | value
    "atLeast(100)"  | 100
    "atLeast( 22 )" | 22
  }

  def 'invalid atLeast matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).errorValue() == error

    where:

    expression     | error
    'atLeast'      | 'Error parsing expression: Was expecting a \'(\' at index 7\n        atLeast\n               ^'
    'atLeast('     | 'Error parsing expression: Was expecting an unsigned number at index 8'
    'atLeast()'    | 'Error parsing expression: Was expecting an unsigned number at index 8'
    'atLeast(100'  | 'Error parsing expression: Was expecting a \')\' at index 11\n        atLeast(100\n                   ^'
    'atLeast(-10)' | 'Error parsing expression: Was expecting an unsigned number at index 8'
    'atLeast(0.1)' | 'Error parsing expression: Was expecting a \')\' at index 9\n        atLeast(0.1)\n                 ^'
  }

  def 'parse atMost matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).value ==
      new MatchingRuleDefinition("", ValueType.Unknown, [ Either.a(new MaxTypeMatcher(value)) ], null, expression.trim())

    where:

    expression     | value
    "atMost(100)"  | 100
    "atMost( 22 )" | 22
  }

  def 'invalid atMost matcher'() {
    expect:
    MatchingRuleDefinition.parseMatchingRuleDefinition(expression).errorValue() == error

    where:

    expression    | error
    'atMost'      | 'Error parsing expression: Was expecting a \'(\' at index 6\n        atMost\n              ^'
    'atMost('     | 'Error parsing expression: Was expecting an unsigned number at index 7'
    'atMost()'    | 'Error parsing expression: Was expecting an unsigned number at index 7'
    'atMost(100'  | 'Error parsing expression: Was expecting a \')\' at index 10\n        atMost(100\n                  ^'
    'atMost(-10)' | 'Error parsing expression: Was expecting an unsigned number at index 7'
    'atMost(0.1)' | 'Error parsing expression: Was expecting a \')\' at index 8\n        atMost(0.1)\n                ^'
  }
}
