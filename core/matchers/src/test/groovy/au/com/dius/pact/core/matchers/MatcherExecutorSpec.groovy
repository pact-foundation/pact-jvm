package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification
import spock.lang.Unroll

import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.DECIMAL
import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.INTEGER
import static au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher.NumberType.NUMBER

@SuppressWarnings(['UnnecessaryBooleanExpression', 'CyclomaticComplexity'])
class MatcherExecutorSpec extends Specification {

  MismatchFactory mismatchFactory
  def path

  static xml(String xml) {
    XmlBodyMatcher.INSTANCE.parse(xml)
  }

  static json(String json) {
    JsonParser.INSTANCE.parseString(json)
  }

  def setup() {
    mismatchFactory = [create: { p0, p1, p2, p3 -> new PathMismatch('', '', p2) } ] as MismatchFactory
    path = ['/']
  }

  @Unroll
  def 'equals matcher matches using equals'() {
    expect:
    MatcherExecutorKt.domatch(EqualsMatcher.INSTANCE, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected                           | actual                             || mustBeEmpty
    '100'                              | '100'                              || true
    100                                | '100'                              || false
    100                                | 100                                || true
    new JsonValue.Integer('100'.chars) | new JsonValue.Integer('100'.chars) || true
    null                               | null                               || true
    '100'                              | null                               || false
    null                               | 100                                || false
    JsonValue.Null.INSTANCE            | null                               || true
    null                               | JsonValue.Null.INSTANCE            || true
    JsonValue.Null.INSTANCE            | JsonValue.Null.INSTANCE            || true
    xml('<a/>')                        | xml('<a/>')                        || true
    xml('<a/>')                        | xml('<b/>')                        || false
    xml('<e xmlns="a"/>')              | xml('<a:e xmlns:a="a"/>')          || true
    xml('<a:e xmlns:a="a"/>')          | xml('<b:e xmlns:b="a"/>')          || true
    xml('<e xmlns="a"/>')              | xml('<e xmlns="b"/>')              || false
    json('"hello"')                    | json('"hello"')                    || true
  }

  @Unroll
  def 'regex matcher matches using the provided regex'() {
    expect:
    MatcherExecutorKt.domatch(new RegexMatcher(regex), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected | actual                               | regex      || mustBeEmpty
    'Harry'  | 'Happy'                              | 'Ha[a-z]*' || true
    'Harry'  | null                                 | 'Ha[a-z]*' || false
    '100'    | 20123                                | '\\d+'     || true
    '100'    | new JsonValue.Integer('20123'.chars) | '\\d+'     || true
  }

  @Unroll
  def 'type matcher matches on types'() {
    expect:
    MatcherExecutorKt.domatch(TypeMatcher.INSTANCE, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected                  | actual                     || mustBeEmpty
    'Harry'                   | 'Some other string'        || true
    100                       | 200.3                      || true
    true                      | false                      || true
    null                      | null                       || true
    '200'                     | 200                        || false
    200                       | null                       || false
    [100, 200, 300]           | [200.3]                    || true
    [a: 100]                  | [a: 200.3, b: 200, c: 300] || true
    JsonValue.Null.INSTANCE   | null                       || true
    null                      | JsonValue.Null.INSTANCE    || true
    JsonValue.Null.INSTANCE   | JsonValue.Null.INSTANCE    || true
    xml('<a/>')               | xml('<a/>')                || true
    xml('<a/>')               | xml('<b/>')                || false
    xml('<e xmlns="a"/>')     | xml('<a:e xmlns:a="a"/>')  || true
    xml('<a:e xmlns:a="a"/>') | xml('<b:e xmlns:b="a"/>')  || true
    xml('<e xmlns="a"/>')     | xml('<e xmlns="b"/>')      || false
    json('"hello"')           | json('"hello"')            || true
    json('100')               | json('200')                || true
  }

  @Unroll
  def 'number type matcher matches on types'() {
    expect:
    MatcherExecutorKt.domatch(new NumberTypeMatcher(numberType), path, expected, actual, mismatchFactory).empty ==
      mustBeEmpty

    where:
    numberType | expected | actual                             || mustBeEmpty
    INTEGER    | 100      | 'Some other string'                || false
    DECIMAL    | 100.0    | 'Some other string'                || false
    NUMBER     | 100      | 'Some other string'                || false
    INTEGER    | 100      | 200.3                              || false
    NUMBER     | 100      | 200.3                              || true
    DECIMAL    | 100.0    | 200.3                              || true
    INTEGER    | 100      | 200                                || true
    INTEGER    | 100      | new JsonValue.Integer('200'.chars) || true
    NUMBER     | 100      | 200                                || true
    DECIMAL    | 100.0    | 200                                || false
    INTEGER    | 100      | false                              || false
    DECIMAL    | 100.0    | false                              || false
    NUMBER     | 100      | false                              || false
    INTEGER    | 100      | null                               || false
    DECIMAL    | 100.0    | null                               || false
    NUMBER     | 100      | null                               || false
    INTEGER    | 100      | [200.3]                            || false
    DECIMAL    | 100.0    | [200.3]                            || false
    NUMBER     | 100      | [200.3]                            || false
    INTEGER    | 100      | [a: 200.3, b: 200, c: 300]         || false
    DECIMAL    | 100.0    | [a: 200.3, b: 200, c: 300]         || false
    NUMBER     | 100      | [a: 200.3, b: 200, c: 300]         || false
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'timestamp matcher'() {
    expect:
    MatcherExecutorKt.domatch(matcher, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected                                      | actual                                  | pattern                          || mustBeEmpty
    '2014-01-01 14:00:00+10:00'                   | '2013-12-01 14:00:00+10:00'             | null                             || true
    '2014-01-01 14:00:00+10:00'                   | 'I\'m a timestamp!'                     | null                             || false
    '2014-01-01 14:00:00+10:00'                   | '2013#12#01#14#00#00'                   | "yyyy'#'MM'#'dd'#'HH'#'mm'#'ss"  || true
    '2014-01-01 14:00:00+10:00'                   | null                                    | null                             || false
    '2014-01-01T10:00+10:00[Australia/Melbourne]' | '2020-01-01T10:00+01:00[Europe/Warsaw]' | "yyyy-MM-dd'T'HH:mmXXX'['zzz']'" || true
    '2019-11-25T13:45:00+02:00'                   | '2019-11-25T11:45:00Z'                  | "yyyy-MM-dd'T'HH:mm:ssX"         || true
    '2019-11-25T13:45:00+02:00'                   | '2019-11-25T11:45:00Z'                  | "yyyy-MM-dd'T'HH:mm:ssZZ"        || true
    '2019-11-25T13:45:00+02:00'                   | '2019-11-25T11:45Z'                     | "yyyy-MM-dd'T'HH:mmZZ"           || true
    '2019-11-25T13:45:00+02:00'                   | '2019-11-25T11Z'                        | "yyyy-MM-dd'T'HHZZ"              || true
    '2019-11-25T13:45:00+0200'                    | '2019-11-25T11:45:00Z'                  | "yyyy-MM-dd'T'HH:mm:ssZ"         || true
    '2019-11-25T13:45:00+0200'                    | '2019-11-25T11:45Z'                     | "yyyy-MM-dd'T'HH:mmZ"            || true
    '2019-11-25T13:45:00+0200'                    | '2019-11-25T11Z'                        | "yyyy-MM-dd'T'HHZ"               || true
    '2019-11-25T13:45:00+0200'                    | '2019-11-25T11:45:00Z'                  | "yyyy-MM-dd'T'HH:mm:ss'Z'"       || true

    matcher = pattern ? new TimestampMatcher(pattern) : new TimestampMatcher()
  }

  @Unroll
  def 'time matcher'() {
    expect:
    MatcherExecutorKt.domatch(matcher, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected         | actual       | pattern       || mustBeEmpty
    '14:00:00'       | '14:00:00'   | null          || true
    '00:00'          | '14:01:02'   | 'mm:ss'       || false
    '00:00:14'       | '05:10:14'   | 'ss:mm:HH'    || true
    '14:00:00+10:00' | null         | null          || false

    matcher = pattern ? new TimeMatcher(pattern) : new TimeMatcher()
  }

  @Unroll
  def 'date matcher'() {
    expect:
    MatcherExecutorKt.domatch(matcher, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected     | actual       | pattern      || mustBeEmpty
    '01-01-1970' | '14-01-2000' | null         || true
    '01-01-1970' | '01011970'   | 'dd-MM-yyyy' || false
    '12/30/1970' | '01/14/2001' | 'MM/dd/yyyy' || true
    '2014-01-01' | null         | null         || false

    matcher = pattern ? new DateMatcher(pattern) : new DateMatcher()
  }

  @Unroll
  def 'include matcher matches if the expected is included in the actual'() {
    expect:
    MatcherExecutorKt.domatch(matcher, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected | actual           || mustBeEmpty
    'Harry'  | 'Harry'          || true
    'Harry'  | 'HarryBob'       || true
    'Harry'  | 'BobHarry'       || true
    'Harry'  | 'BobHarryGeorge' || true
    'Harry'  | 'Tom'            || false
    'Harry'  | null             || false
    '100'    | 2010023          || true

    matcher = new IncludeMatcher(expected)
  }

  def 'equality matching produces a message on mismatch'() {
    given:
    def factory = Mock MismatchFactory

    when:
    MatcherExecutorKt.matchEquality path, 'foo', 'bar', factory

    then:
    1 * factory.create(_, _, "Expected 'bar' to equal 'foo'", _)
    0 * _
  }

  @Unroll
  def 'list type matcher matches on array sizes - #matcher'() {
    expect:
    MatcherExecutorKt.domatch(matcher, path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    matcher                     | expected | actual    || mustBeEmpty
    TypeMatcher.INSTANCE        | [0]      | [1]       || true
    new MinTypeMatcher(1)       | [0]      | [1]       || true
    new MinTypeMatcher(2)       | [0, 1]   | [1]       || false
    new MaxTypeMatcher(2)       | [0]      | [1]       || true
    new MaxTypeMatcher(1)       | [0]      | [1, 1]    || false
    new MinMaxTypeMatcher(1, 2) | [0]      | [1]       || true
    new MinMaxTypeMatcher(2, 3) | [0, 1]   | [1]       || false
    new MinMaxTypeMatcher(1, 2) | [0, 1]   | [1, 1]    || true
    new MinMaxTypeMatcher(1, 2) | [0]      | [1, 1, 2] || false
  }

  @Unroll
  @SuppressWarnings('UnnecessaryCast')
  def 'matching integer values'() {
    expect:
    MatcherExecutorKt.matchInteger(value) == result

    where:

    value             | result
    '100'             | false
    100               | true
    100.0             | false
    100 as int        | true
    100 as long       | true
    100 as BigInteger | true
    100 as BigDecimal | true
    BigInteger.ZERO   | true
    BigDecimal.ZERO   | true
  }

  @Unroll
  @SuppressWarnings('UnnecessaryCast')
  def 'matching decimal number values'() {
    expect:
    MatcherExecutorKt.matchDecimal(value) == result

    where:

    value                            | result
    new JsonValue.Decimal('0'.chars) | true
    '100'                            | false
    100                              | false
    100.0                            | true
    100.0 as float                   | true
    100.0 as double                  | true
    100 as int                       | false
    100 as long                      | false
    100 as BigInteger                | false
    BigInteger.ZERO                  | false
    BigDecimal.ZERO                  | true
  }

  @Unroll
  def 'display #value as #display'() {
    expect:
    MatcherExecutorKt.valueOf(value) == display

    where:

    value                         || display
    null                          || 'null'
    'foo'                         || "'foo'"
    55                            || '55'
    xml('<foo/>')                 || '<foo>'
    xml('<foo><bar/></foo>')      || '<foo>'
    xml('<foo xmlns="a"/>')       || '<{a}foo>'
    xml('<a>text</a>').firstChild || "'text'"
  }

}
