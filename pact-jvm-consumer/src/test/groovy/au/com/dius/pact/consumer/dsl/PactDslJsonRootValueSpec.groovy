package au.com.dius.pact.consumer.dsl

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PactDslJsonRootValueSpec extends Specification {
  @SuppressWarnings('PrivateFieldCouldBeFinal')
  @Shared
  private Date date = new Date(100, 1, 1, 20, 0, 0)

  @Unroll
  def 'correctly converts the value #value to JSON'() {
    expect:
    value.body as String == json

    where:

    value                                                             | json
    PactDslJsonRootValue.stringType('TEST')                           | '"TEST"'
    PactDslJsonRootValue.numberType(100)                              | '100'
    PactDslJsonRootValue.integerType(100)                             | '100'
    PactDslJsonRootValue.decimalType(100)                             | '100.0'
    PactDslJsonRootValue.booleanType(true)                            | 'true'
    PactDslJsonRootValue.stringMatcher('\\w+', 'test')                | '"test"'
    PactDslJsonRootValue.timestamp('yyyy-MM-dd HH:mm:ss', date)       | '"2000-02-01 20:00:00"'
    PactDslJsonRootValue.time('HH:mm:ss', date)                       | '"20:00:00"'
    PactDslJsonRootValue.date('yyyy-MM-dd', date)                     | '"2000-02-01"'
    PactDslJsonRootValue.ipAddress()                                  | '"127.0.0.1"'
    PactDslJsonRootValue.id(1000)                                     | '1000'
    PactDslJsonRootValue.hexValue('1000')                             | '"1000"'
    PactDslJsonRootValue.uuid('e87f3c51-545c-4bc2-b1b5-284de67d627e') | '"e87f3c51-545c-4bc2-b1b5-284de67d627e"'
  }

}
