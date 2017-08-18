package au.com.dius.pact.consumer

import spock.lang.Specification
import spock.lang.Unroll

class MockHttpServerSpec extends Specification {

  @Unroll
  def 'calculated charset test - "#contentTypeHeader"'() {

    expect:
    MockHttpServerKt.calculateCharset(headers).name() == expectedCharset

    where:

    contentTypeHeader               | expectedCharset
    null                            | 'UTF-8'
    'null'                          | 'UTF-8'
    ''                              | 'UTF-8'
    'text/plain'                    | 'UTF-8'
    'text/plain; charset'           | 'UTF-8'
    'text/plain; charset='          | 'UTF-8'
    'text/plain;charset=ISO-8859-1' | 'ISO-8859-1'

    headers = ['Content-Type': contentTypeHeader]

  }

  def 'with no content type defaults to UTF-8'() {
    expect:
    MockHttpServerKt.calculateCharset([:]).name() == 'UTF-8'
  }

  def 'ignores case with the header name'() {
    expect:
    MockHttpServerKt.calculateCharset(['content-type': 'text/plain; charset=ISO-8859-1']).name() == 'ISO-8859-1'
  }

}
