package au.com.dius.pact.core.model

import spock.lang.Issue
import spock.lang.Specification

import static au.com.dius.pact.core.model.PactReaderKt.queryStringToMap

class PactReaderKtSpec extends Specification {
  def 'parsing a query string'() {
    expect:
    queryStringToMap(query) == result

    where:

    query          | result
    null           | [:]
    ''             | [:]
    'p=1'          | [p: ['1']]
    'p=1&q=2'      | [p: ['1'], q: ['2']]
    'p=1&q=2&p=3'  | [p: ['1', '3'], q: ['2']]
    'p=1&q=2=&p=3' | [p: ['1', '3'], q: ['2=']]
    '&&'           | [:]
  }

  @Issue('#1788')
  def 'parsing a query string with empty or missing values'() {
    expect:
    queryStringToMap(query) == result

    where:

    query        | result
    'p='         | [p: ['']]
    'p=1&q=&p=3' | [p: ['1', '3'], q: ['']]
    'p&q=1&q=2'  | [p: [null], q: ['1', '2']]
    'p&p&p'      | [p: [null, null, null]]
  }
}
