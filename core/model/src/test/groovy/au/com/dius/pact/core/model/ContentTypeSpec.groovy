package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import java.nio.charset.Charset

@SuppressWarnings('UnnecessaryBooleanExpression')
@RestoreSystemProperties
class ContentTypeSpec extends Specification {

  def setupSpec() {
    System.setProperty('pact.content_type.override.application/x-thrift', 'json')
    System.setProperty('pact.content_type.override.application/x-other', 'text')
    System.setProperty('pact.content_type.override.application/x-bin', 'binary')
    System.setProperty('pact.content_type.override.application/x-ml', 'xml')
  }

  @Unroll
  def '"#value" is json -> #result'() {
    expect:
    result == contentType.json

    where:

    value                                    || result
    ''                                       || false
    'text/plain'                             || false
    'application/pdf'                        || false
    'application/json'                       || true
    'application/hal+json'                   || true
    'application/HAL+JSON'                   || true
    'application/vnd.schemaregistry.v1+json' || false
    'application/x-thrift'                   || true
    'application/x-other'                    || false
    'application/graphql'                    || true

    contentType = new ContentType(value)
  }

  @Unroll
  def '"#value" is kafka schema registry -> #result'() {
    expect:
    result == contentType.kafkaSchemaRegistryJson

    where:

    value                                    || result
    ''                                       || false
    'text/plain'                             || false
    'application/pdf'                        || false
    'application/json'                       || false
    'application/hal+json'                   || false
    'application/HAL+JSON'                   || false
    'application/vnd.schemaregistry.v1+json' || true
    'application/x-thrift'                   || false
    'application/x-other'                    || false
    'application/graphql'                    || false
    'application/xml'                        || false

    contentType = new ContentType(value)
  }

  @Unroll
  def '"#value" is xml -> #result'() {
    expect:
    result == contentType.xml

    where:

    value                   || result
    ''                      || false
    'text/plain'            || false
    'application/pdf'       || false
    'application/xml'       || true
    'application/stuff+xml' || true
    'application/STUFF+XML' || true
    'application/x-ml'      || true
    'application/x-thrift'  || false

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

  @Unroll
  def '"#value" is binary -> #result'() {
    expect:
    contentType.binaryType == result

    where:

    value                               || result
    ''                                  || false
    'text/plain'                        || false
    'application/pdf'                   || true
    'application/zip'                   || true
    'application/json'                  || false
    'application/hal+json'              || false
    'application/HAL+JSON'              || false
    'application/xml'                   || false
    'application/atom+xml'              || false
    'application/octet-stream'          || true
    'image/jpeg'                        || true
    'video/H264'                        || true
    'audio/aac'                         || true
    'text/csv'                          || false
    'multipart/form-data'               || true
    'application/x-www-form-urlencoded' || false
    'application/x-bin'                 || true

    contentType = new ContentType(value)
  }

  @Unroll
  def '"#value" supertype -> #result'() {
    expect:
    contentType.supertype?.asString() == result

    where:

    value                               || result
    ''                                  || null
    'text/plain'                        || 'application/octet-stream'
    'application/pdf'                   || 'application/octet-stream'
    'application/zip'                   || 'application/octet-stream'
    'application/json'                  || 'application/javascript'
    'application/hal+json'              || 'application/json'
    'application/HAL+JSON'              || 'application/json'
    'application/xml'                   || 'text/plain'
    'application/atom+xml'              || 'application/xml'
    'application/octet-stream'          || null
    'image/jpeg'                        || 'application/octet-stream'
    'video/H264'                        || 'application/octet-stream'
    'audio/aac'                         || 'application/octet-stream'
    'text/csv'                          || 'text/plain'
    'multipart/form-data'               || 'application/octet-stream'
    'application/x-www-form-urlencoded' || 'text/plain'

    contentType = new ContentType(value)
  }
}
