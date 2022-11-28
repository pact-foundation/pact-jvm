package au.com.dius.pact.core.matchers.util

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('ClosureAsLastMethodParameter')
class CollectionUtilsSpec extends Specification {

  @Unroll
  def 'padTo test'() {
    expect:
    CollectionUtilsKt.padTo(list, size, 1) == result

    where:

    list      | size | result
    []        | 0    | []
    [1]       | 1    | [1]
    [1]       | 0    | []
    [1, 2, 3] | 0    | []
    [1, 2, 3] | 2    | [1, 2]
    [1, 2, 3] | 3    | [1, 2, 3]
    []        | 3    | [1, 1, 1]
    [1]       | 3    | [1, 1, 1]
    [1, 2, 3] | 6    | [1, 2, 3, 1, 1, 1]
  }

}
