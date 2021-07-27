package au.com.dius.pact.core.support

import kotlin.Pair
import spock.lang.Specification
import spock.lang.Unroll
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok

@SuppressWarnings('UnnecessaryBooleanExpression')
class UtilsSpec extends Specification {

  @Unroll
  def 'lookupInMap'() {
    expect:
    Utils.INSTANCE.lookupInMap(map, 'key', clazz, defaultVal) == value

    where:

    map          | clazz   | defaultVal || value
    [:]          | Boolean | true       || true
    [key: '']    | Boolean | true       || true
    [key: false] | Boolean | true       || false
    [key: null]  | Boolean | true       || true
  }

  def 'permutations'() {
    given:
    List<Integer> list1 = [1, 2, 3]
    List<String> list2 = ['A', 'B']

    when:
    def result = Utils.INSTANCE.permutations(list1, list2)

    then:
    result == [
      new Pair(1, 'A'),
      new Pair(1, 'B'),
      new Pair(2, 'A'),
      new Pair(2, 'B'),
      new Pair(3, 'A'),
      new Pair(3, 'B')
    ]
  }

  def 'permutations when the first list is empty'() {
    given:
    List<Integer> list1 = []
    List<String> list2 = ['A', 'B']

    when:
    def result = Utils.INSTANCE.permutations(list1, list2)

    then:
    result == [
      new Pair(null, 'A'),
      new Pair(null, 'B')
    ]
  }

  def 'permutations when the second list is empty'() {
    given:
    List<Integer> list1 = [1, 2, 3]
    List<String> list2 = []

    when:
    def result = Utils.INSTANCE.permutations(list1, list2)

    then:
    result == [
      new Pair(1, null),
      new Pair(2, null),
      new Pair(3, null)
    ]
  }

  def 'permutations when the lists are empty'() {
    given:
    List<Integer> list1 = []
    List<String> list2 = []

    when:
    def result = Utils.INSTANCE.permutations(list1, list2)

    then:
    result == []
  }

  @Unroll
  def 'sizeOf "#size"'() {
    expect:
    Utils.INSTANCE.sizeOf(size) == result

    where:

    size       || result
    ''         || new Err("'' is not a valid data size")
    '100'      || new Err("'100' is not a valid data size")
    'aksldjsk' || new Err("'aksldjsk' is not a valid data size")
    '-kb'      || new Err("'-kb' is not a valid data size")
    '-1kb'     || new Err("'-1kb' is not a valid data size")
    '22b'      || new Ok(22)
    '2kb'      || new Ok(2048)
    '2KB'      || new Ok(2048)
    '2mb'      || new Ok(2048 * 1024)
  }
}
