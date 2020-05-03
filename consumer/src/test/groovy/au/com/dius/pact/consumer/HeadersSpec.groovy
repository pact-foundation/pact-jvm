package au.com.dius.pact.consumer

import spock.lang.Specification
import spock.lang.Unroll

class HeadersSpec extends Specification {

  @Unroll
  def 'multi-part header regex test'() {
    expect:
    value.matches(Headers.MULTIPART_HEADER_REGEX) == matches

    where:

    value | matches
    ''    | false
    'multipart/form-data;boundary=XYZ' | true
    'multipart/form-data; boundary=XYZ' | true
    'multipart/form-data;charset=UTF-8;boundary=XYZ' | true
    'multipart/form-data; charset=UTF-8; boundary=XYZ' | true
  }

}
