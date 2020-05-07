package au.com.dius.pact.core.support

import spock.lang.Specification
import spock.lang.Unroll

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

}
