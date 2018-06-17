package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

class HttpPartSpec extends Specification {

  @SuppressWarnings('LineLength')
  @Unroll
  def 'Pact mimeType'() {
    expect:
    request.mimeType() == mimeType

    where:
    request                                                                           | mimeType
    new Request('Get', '')                                                            | 'text/plain'
    new Request('Get', '', null, ['Content-Type': 'text/html'])                       | 'text/html'
    new Request('Get', '', null, ['Content-Type': 'application/json; charset=UTF-8']) | 'application/json'
    new Request('Get', '', null, ['content-type': 'application/json'])                | 'application/json'
    new Request('Get', '', null, ['CONTENT-TYPE': 'application/json'])                | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('{"json": true}'))           | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('{}'))                       | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('[]'))                       | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('[1,2,3]'))                  | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('"string"'))                                                   | 'application/json'
    new Request('Get', '', null, null, OptionalBody.body('<?xml version="1.0" encoding="UTF-8"?>\n<json>false</json>')) | 'application/xml'
    new Request('Get', '', null, null, OptionalBody.body('<json>false</json>'))                                         | 'application/xml'
    new Request('Get', '', null, null, OptionalBody.body('this is not json'))                                           | 'text/plain'
    new Request('Get', '', null, null, OptionalBody.body('<html><body>this is also not json</body></html>'))            | 'text/html'
  }

  @SuppressWarnings('LineLength')
  @Unroll
  def 'Pact charset'() {
    expect:
    request.charset() == charset

    where:
    request                                                                                                             | charset
    new Request('Get', '')                                                                                              | null
    new Request('Get', '', null, ['Content-Type': 'text/html'])                                                         | null
    new Request('Get', '', null, ['Content-Type': 'application/json; charset=UTF-8'])                                   | Charset.forName('UTF-8')
  }
}
