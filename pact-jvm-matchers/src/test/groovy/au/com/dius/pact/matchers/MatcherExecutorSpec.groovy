package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.DateMatcher
import au.com.dius.pact.model.matchingrules.EqualsMatcher
import au.com.dius.pact.model.matchingrules.RegexMatcher
import au.com.dius.pact.model.matchingrules.TimeMatcher
import au.com.dius.pact.model.matchingrules.TimestampMatcher
import au.com.dius.pact.model.matchingrules.TypeMatcher
import scala.collection.JavaConversions
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('UnnecessaryBooleanExpression')
class MatcherExecutorSpec extends Specification {

  def mismatchFactory
  def path

  def setup() {
    mismatchFactory = [create: { p0, p1, p2, p3 -> 'mismatch' } ] as MismatchFactory
    path = JavaConversions.asScalaBuffer(['/']).toSeq()
  }

  @Unroll
  def 'equals matcher matches using equals'() {
    expect:
    MatcherExecutor.domatch(new EqualsMatcher(), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected | actual || mustBeEmpty
    '100'    | '100'  || true
    100      | '100'  || false
  }

  @Unroll
  def 'regex matcher matches using the provided regex'() {
    expect:
    MatcherExecutor.domatch(new RegexMatcher(regex), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected | actual  | regex      || mustBeEmpty
    'Harry'  | 'Happy' | 'Ha[a-z]*' || true
    'Harry'  | null    | 'Ha[a-z]*' || false
  }

  @Unroll
  def 'type matcher matches on types'() {
    expect:
    MatcherExecutor.domatch(new TypeMatcher(), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected        | actual                     || mustBeEmpty
    'Harry'         | 'Some other string'        || true
    100             | 200.3                      || true
    true            | false                      || true
    null            | null                       || true
    '200'           | 200                        || false
    200             | null                       || false
    [100, 200, 300] | [200.3]                    || true
    [a: 100]        | [a: 200.3, b: 200, c: 300] || true
  }

  @Unroll
  def 'timestamp matcher'() {
    expect:
    MatcherExecutor.domatch(new TimestampMatcher(pattern), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected                    | actual                      | pattern               || mustBeEmpty
    '2014-01-01 14:00:00+10:00' | '2013-12-01 14:00:00+10:00' | null                  || true
    '2014-01-01 14:00:00+10:00' | 'I\'m a timestamp!'         | null                  || false
    '2014-01-01 14:00:00+10:00' | '2013#12#01#14#00#00'       | 'yyyy#MM#dd#HH#mm#ss' || true
    '2014-01-01 14:00:00+10:00' | null                        | null                  || false
  }

  @Unroll
  def 'time matcher'() {
    expect:
    MatcherExecutor.domatch(new TimeMatcher(pattern), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected         | actual     | pattern    || mustBeEmpty
    '14:00:00'       | '14:00:00' | null       || true
    '00:00'          | '14:01:02' | 'mm:ss'    || false
    '00:00:14'       | '05:10:14' | 'ss:mm:HH' || true
    '14:00:00+10:00' | null       | null       || false
  }

  @Unroll
  def 'date matcher'() {
    expect:
    MatcherExecutor.domatch(new DateMatcher(pattern), path, expected, actual, mismatchFactory).empty == mustBeEmpty

    where:
    expected     | actual       | pattern      || mustBeEmpty
    '01-01-1970' | '14-01-2000' | null         || true
    '01-01-1970' | '01011970'   | 'dd-MM-yyyy' || false
    '12/30/1970' | '01/14/2001' | 'MM/dd/yyyy' || true
    '2014-01-01' | null         | null         || false
  }

}
