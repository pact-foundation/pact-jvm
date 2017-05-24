package au.com.dius.pact.consumer.groovy

import spock.lang.Specification
import spock.lang.Unroll

class MatchersSpec extends Specification {

  @Unroll
  def 'matcher methods generate the correct matcher definition - #matcherMethod'() {
    expect:
    Matchers."$matcherMethod"(param).matcher.toMap() == matcherDefinition

    where:

    matcherMethod | param        | matcherDefinition
    'string'      | 'type'       | [match: 'type']
    'identifier'  | ''           | [match: 'integer']
    'numeric'     | 1            | [match: 'number']
    'decimal'     | 1            | [match: 'decimal']
    'integer'     | 1            | [match: 'integer']
    'regexp'      | '[0-9]+'     | [match: 'regex', regex: '[0-9]+']
    'hexValue'    | '1234'       | [match: 'regex', regex: '[0-9a-fA-F]+']
    'ipAddress'   | '1.2.3.4'    | [match: 'regex', regex: '(\\d{1,3}\\.)+\\d{1,3}']
    'timestamp'   | 'yyyy-mm-dd' | [match: 'timestamp', timestamp: 'yyyy-mm-dd']
    'date'        | 'yyyy-mm-dd' | [match: 'date', date: 'yyyy-mm-dd']
    'time'        | 'yyyy-mm-dd' | [match: 'time', time: 'yyyy-mm-dd']

  }

}
