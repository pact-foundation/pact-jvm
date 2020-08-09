package au.com.dius.pact.core.matchers.util

import spock.lang.Specification
import spock.lang.Unroll

class IndicesCombinationSpec extends Specification {
  IndicesCombination combo

  def 'produces correct sequence of indices for collection'() {
    given:
    def list = [1, 2, 3]
    combo = IndicesCombination.of(list)

    expect:
    combo.indices().collect() == [0, 1, 2]
  }

  @Unroll
  def 'produces correct sequence of #n indices'() {
    given:
    combo = IndicesCombination.of(n)

    expect:
    combo.indices().collect() == (0..<n)

    where:
    n << [0, 1, 2, 32, 33]
  }

  @Unroll
  def 'removes #remove from collection of #n indices'() {
    given:
    combo = IndicesCombination.of(n)

    when:
    for (item in remove) {
      combo = combo - item
    }

    then:
    combo.indices().collect() == expected

    where:
    n  | remove    | expected
    3  | [0]       | [1, 2]
    3  | [1]       | [0, 2]
    3  | [2]       | [0, 1]
    3  | [0, 2]    | [1]
    3  | [0, 1, 2] | []
    36 | [5, 33]   | (0..4) + (6..32) + [34, 35]
  }
}
