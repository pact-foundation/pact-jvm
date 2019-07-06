package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.model.PactSpecVersion
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
    value.with { it.setEncodeJson(true); it }.body as String == json

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

  def 'support for date and time expressions'() {
    given:
    def date = PactDslJsonRootValue.dateExpression('today + 1 day')
    def time = PactDslJsonRootValue.timeExpression('now + 1 hour')
    def datetime = PactDslJsonRootValue.datetimeExpression('today + 1 hour')

    expect:
    date.matchers.toMap(PactSpecVersion.V3) == [matchers: [[match: 'date', date: 'yyyy-MM-dd']], combine: 'AND']
    date.generators.toMap(PactSpecVersion.V3) == [body: [
      '': [type: 'Date', format: 'yyyy-MM-dd', expression: 'today + 1 day']]]

    time.matchers.toMap(PactSpecVersion.V3) == [matchers: [[match: 'time', time: 'HH:mm:ss']], combine: 'AND']
    time.generators.toMap(PactSpecVersion.V3) == [body: [
      '': [type: 'Time', format: 'HH:mm:ss', expression: 'now + 1 hour']]]

    datetime.matchers.toMap(PactSpecVersion.V3) == [matchers: [[
      match: 'timestamp', timestamp: "yyyy-MM-dd'T'HH:mm:ss"]], combine: 'AND']
    datetime.generators.toMap(PactSpecVersion.V3) == [body: [
      '': [type: 'DateTime', format: 'yyyy-MM-dd\'T\'HH:mm:ss', expression: 'today + 1 hour']]]
  }

}
