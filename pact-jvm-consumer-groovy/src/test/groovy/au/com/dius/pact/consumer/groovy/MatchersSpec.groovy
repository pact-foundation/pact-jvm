package au.com.dius.pact.consumer.groovy

import spock.lang.Specification
import spock.lang.Unroll

class MatchersSpec extends Specification {

  @Unroll
  @SuppressWarnings('LineLength')
  def 'matcher methods generate the correct matcher definition - #matcherMethod'() {
    expect:
    Matchers."$matcherMethod"(param).matcher.toMap() == matcherDefinition

    where:

    matcherMethod | param                                  | matcherDefinition
    'string'      | 'type'                                 | [match: 'type']
    'identifier'  | ''                                     | [match: 'integer']
    'numeric'     | 1                                      | [match: 'number']
    'decimal'     | 1                                      | [match: 'decimal']
    'integer'     | 1                                      | [match: 'integer']
    'regexp'      | '[0-9]+'                               | [match: 'regex', regex: '[0-9]+']
    'hexValue'    | '1234'                                 | [match: 'regex', regex: '[0-9a-fA-F]+']
    'ipAddress'   | '1.2.3.4'                              | [match: 'regex', regex: '(\\d{1,3}\\.)+\\d{1,3}']
    'timestamp'   | 'yyyy-mm-dd'                           | [match: 'timestamp', timestamp: 'yyyy-mm-dd']
    'date'        | 'yyyy-mm-dd'                           | [match: 'date', date: 'yyyy-mm-dd']
    'time'        | 'yyyy-mm-dd'                           | [match: 'time', time: 'yyyy-mm-dd']
    'uuid'        | '12345678-1234-1234-1234-123456789012' | [match: 'regex', regex: '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}']
    'equalTo'     | 'value'                                | [match: 'equality']
    'includesStr' | 'value'                                | [match: 'include', value: 'value']
  }

  @Unroll
  def 'like matcher methods generate the correct matcher definition - #matcherMethod'() {
    expect:
    Matchers."$matcherMethod"(param, [:]).matcher.toMap() == matcherDefinition

    where:

    matcherMethod | param | matcherDefinition
    'maxLike'     | 10    | [match: 'type', max: 10]
    'minLike'     | 10    | [match: 'type', min: 10]
  }

  def 'each like matcher method generates the correct matcher definition'() {
    expect:
    Matchers.eachLike([:]).matcher.toMap() == [match: 'type']
  }

  @Unroll
  def 'string matcher should use provided value - `#value`'() {
    expect:
    Matchers.string(value).value == value

    where:
    value << ['', ' ', 'example']
  }

  def 'string matcher should generate value when not provided'() {
    expect:
    !Matchers.string().value.isEmpty()
  }
}
