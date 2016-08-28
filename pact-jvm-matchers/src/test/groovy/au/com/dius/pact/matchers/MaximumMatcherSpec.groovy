package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MaxTypeMatcher
import scala.collection.JavaConversions
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('UnnecessaryBooleanExpression')
class MaximumMatcherSpec extends Specification {

  def mismatchFactory
  def path

  def setup() {
    mismatchFactory = [create: { p0, p1, p2, p3 -> 'mismatch' } ] as MismatchFactory
    path = JavaConversions.asScalaBuffer(['$', 'animals', '0']).toSeq()
  }

  @Unroll
  def 'with an array match if the array #condition'() {
    expect:
    MatcherExecutor.domatch(new MaxTypeMatcher(2), path, expected, actual, mismatchFactory).empty

    where:
    condition    | expected | actual
    'is smaller' | [1, 2]   | [1]
    'is the correct size' | [1, 2]   | [1, 3]
  }

  @Unroll
  def 'with an array not match if the array #condition'() {
    expect:
    !MatcherExecutor.domatch(new MaxTypeMatcher(2), path, expected, actual, mismatchFactory).empty

    where:
    condition   | expected | actual
    'is larger' | [1, 2]   | [1, 2, 3]
  }

  @Unroll
  def 'with a non array default to a type matcher'() {
    expect:
    MatcherExecutor.domatch(new MaxTypeMatcher(2), path, expected, actual, mismatchFactory).empty == beEmpty

    where:
    expected | actual   || beEmpty
    'Fred'   | 'George' || true
    'Fred'   | 100      || false
  }

}
