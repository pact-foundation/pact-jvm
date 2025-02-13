package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['UnnecessaryBooleanExpression', 'LineLength'])
class MinimumMatcherSpec extends Specification {

  def mismatchFactory
  def path

  def setup() {
    mismatchFactory = [create: { p0, p1, p2, p3 -> new StatusMismatch(1, 1, null, []) } ] as MismatchFactory
    path = ['$', 'animals', '0']
  }

  @Unroll
  def 'with an array match if the array #condition'() {
    expect:
    MatcherExecutorKt.domatch(new MinTypeMatcher(2), path, expected, actual, mismatchFactory, false, null).empty

    where:
    condition             | expected | actual
    'is larger'           | [1, 2]   | [1, 2, 3]
    'is the correct size' | [1, 2]   | [1, 3]
  }

  @Unroll
  def 'with an array not match if the array #condition'() {
    expect:
    !MatcherExecutorKt.domatch(new MinTypeMatcher(2), path, expected, actual, mismatchFactory, false, null).empty

    where:
    condition    | expected | actual
    'is smaller' | [1, 2]   | [1]
  }

  @Unroll
  def 'with a non array default to a type matcher'() {
    expect:
    MatcherExecutorKt.domatch(new MinTypeMatcher(2), path, expected, actual, mismatchFactory, false, null).empty
      == beEmpty

    where:
    expected | actual   || beEmpty
    'Fred'   | 'George' || true
    'Fred'   | 100      || false
  }

  def 'when cascaded, do not apply the limits'() {
    given:
    def expected = [1, 2]
    def actual = [1]

    expect:
    MatcherExecutorKt.domatch(new MinTypeMatcher(2), path, expected, actual, mismatchFactory, true, null).empty
  }
}
