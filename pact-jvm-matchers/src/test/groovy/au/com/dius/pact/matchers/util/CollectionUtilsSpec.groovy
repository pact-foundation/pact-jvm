package au.com.dius.pact.matchers.util

import spock.lang.Specification

@SuppressWarnings('ClosureAsLastMethodParameter')
class CollectionUtilsSpec extends Specification {

  def 'tails test'() {
    expect:
    CollectionUtilsKt.tails(['a', 'b', 'c', 'd']) == [['a', 'b', 'c', 'd'], ['b', 'c', 'd'], ['c', 'd'], ['d'], []]
    CollectionUtilsKt.tails(['something', '$']) == [['something', '$'], ['$'], []]
  }

  def 'corresponds test'() {
    expect:
    CollectionUtilsKt.<Integer, String>corresponds([1, 2, 3], ['1', '2', '3'], { a, b -> a == Integer.parseInt(b) })
    !CollectionUtilsKt.<Integer, String>corresponds([1, 2, 4], ['1', '2', '3'], { a, b -> a == Integer.parseInt(b) })
    !CollectionUtilsKt.<Integer, String>corresponds([1, 2, 3, 4], ['1', '2', '3'], { a, b -> a == Integer.parseInt(b) })
  }

}
