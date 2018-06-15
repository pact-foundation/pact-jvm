package au.com.dius.pact.model

import au.com.dius.pact.core.model.ContentType
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('UnnecessaryBooleanExpression')
class ContentTypeSpec extends Specification {

  @Unroll
  def '"#value" is json -> #result'() {
    expect:
    result ? contentType.isJson() : !contentType.isJson()

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
    result ? contentType.isXml() : !contentType.isXml()

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

}
