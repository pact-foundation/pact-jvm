package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

@SuppressWarnings('UnnecessaryBooleanExpression')
class ContentTypeSpec extends Specification {

  @Unroll
  def '"#value" is json -> #result'() {
    expect:
    result ? contentType.json : !contentType.json

    where:

    value                  || result
    ''                     || false
    'text/plain'           || false
    'application/pdf'      || false
    'application/json'     || true
    'application/hal+json' || true
    'application/HAL+JSON' || true

    contentType = new ContentType(value)
  }

  @Unroll
  def '"#value" is xml -> #result'() {
    expect:
    result ? contentType.xml : !contentType.xml

    where:

    value                   || result
    ''                      || false
    'text/plain'            || false
    'application/pdf'       || false
    'application/xml'       || true
    'application/stuff+xml' || true
    'application/STUFF+XML' || true

    contentType = new ContentType(value)
  }

  @Unroll
  def '"#value" charset -> #result'() {
    expect:
    contentType.asCharset() == result

    where:

    value                              || result
    ''                                 || Charset.defaultCharset()
    'text/plain'                       || Charset.defaultCharset()
    'application/pdf;a=b'              || Charset.defaultCharset()
    'application/xml ; charset=UTF-16' || Charset.forName('UTF-16')

    contentType = new ContentType(value)
  }

}
