package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import spock.lang.Specification
import spock.lang.Unroll

class MinEqualsIgnoreOrderMatcherSpec extends Specification {

  def mismatchFactory
  def path

  def setup() {
    mismatchFactory = [create: { p0, p1, p2, p3 -> new StatusMismatch(1, 1, null, []) }] as MismatchFactory
    path = ['$', 'animals', '0']
  }

  @Unroll
  def 'with an array match if the actual #condition'() {
    when:
    def mismatches =
        MatcherExecutorKt.domatch(new MinEqualsIgnoreOrderMatcher(2), path, expected, actual, mismatchFactory, false)

    then:
    mismatches.empty == match

    where:
    condition           | expected | actual    | match
    'is larger'         | [1, 2]   | [1, 2, 3] | true
    'is correct size'   | [1, 2]   | [1, 2]    | true
    'is larger, mixed'  | [1, 2]   | [2, 1, 3] | true
    'is correct, mixed' | [1, 2]   | [2, 1]    | true
    'is smaller'        | [1, 2]   | [1]       | false
  }

  @Unroll
  def 'with a non array default to a equality matcher'() {
    when:
    def mismatches =
        MatcherExecutorKt.domatch(new MinEqualsIgnoreOrderMatcher(2), path, expected, actual, mismatchFactory, false)

    then:
    mismatches.empty == match

    where:
    expected | actual | match
    'Fred'   | 'Fred' | true
    'Fred'   | 100    | false
  }
}
