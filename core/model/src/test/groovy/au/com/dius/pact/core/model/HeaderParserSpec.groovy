package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class HeaderParserSpec extends Specification {

  private static final String ACCEPT =
    'application/prs.hal-forms+json;q=1.0, application/hal+json;q=0.9, application/vnd.api+json;q=0.8, application/vnd.siren+json;q=0.8, application/vnd.collection+json;q=0.8, application/json;q=0.7, text/html;q=0.6, application/vnd.pactbrokerextended.v1+json;q=1.0'

  @Unroll
  def 'loading string headers from JSON - #desc'() {
    expect:
    HeaderParser.INSTANCE.fromJson(key, new JsonValue.StringValue(value)) == result

    where:

    desc            | key       | value | result
    'simple header' | 'HeaderA' | 'A'   | ['A']
    'date header' | 'date' | 'Sat, 24 Jul 2021 04:16:53 GMT'   | ['Sat, 24 Jul 2021 04:16:53 GMT']
    'header with parameter' | 'content-type' | 'text/html; charset=utf-8' | ['text/html;charset=utf-8']
    'header with multiple values' | 'access-control-allow-methods' | 'POST, GET, PUT, HEAD, DELETE, OPTIONS, PATCH' | ['POST', 'GET', 'PUT', 'HEAD', 'DELETE', 'OPTIONS', 'PATCH']
    'header with multiple values with parameters' | 'Accept' | ACCEPT | ['application/prs.hal-forms+json;q=1.0', 'application/hal+json;q=0.9', 'application/vnd.api+json;q=0.8', 'application/vnd.siren+json;q=0.8', 'application/vnd.collection+json;q=0.8', 'application/json;q=0.7', 'text/html;q=0.6', 'application/vnd.pactbrokerextended.v1+json;q=1.0']
  }
}
