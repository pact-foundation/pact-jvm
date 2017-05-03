package au.com.dius.pact.consumer

import spock.lang.Specification
import spock.lang.Unroll

class MockHttpServerSpec extends Specification {

  @Unroll
  def 'calculated charset test - "#contentTypeHeader"'() {

    expect:
    MockHttpServerKt.calculateCharset(headers).name() == expectedCharset

    where:

    contentTypeHeader          | expectedCharset
    null                       | 'ISO-8859-1'
    'null'                     | 'ISO-8859-1'
    ''                         | 'ISO-8859-1'
    'text/plain'               | 'ISO-8859-1'
    'text/plain; charset'      | 'ISO-8859-1'
    'text/plain; charset='     | 'ISO-8859-1'
    'text/plain;charset=UTF-8' | 'UTF-8'

    headers = ['Content-Type': contentTypeHeader]

  }

  def 'with no content type defaults to ISO-8859-1'() {
    expect:
    MockHttpServerKt.calculateCharset([:]).name() == 'ISO-8859-1'
  }

  def 'ignores case with the header name'() {
    expect:
    MockHttpServerKt.calculateCharset(['content-type': 'text/plain; charset=UTF-8']).name() == 'UTF-8'
  }

}
