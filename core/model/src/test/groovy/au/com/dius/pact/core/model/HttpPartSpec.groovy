package au.com.dius.pact.core.model

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

class HttpPartSpec extends Specification {

  @SuppressWarnings('LineLength')
  @Unroll
  def 'Pact contentType'() {
    expect:
    request.contentType() == contentType

    where:
    request                                                                                                                 | contentType
    new Request('Get', '')                                                                                                  | null
    new Request('Get', '', [:], ['Content-Type': ['text/html']])                                                            | 'text/html'
    new Request('Get', '', [:], ['Content-Type': ['application/json; charset=UTF-8']])                                      | 'application/json'
    new Request('Get', '', [:], ['content-type': ['application/json']])                                                     | 'application/json'
    new Request('Get', '', [:], ['CONTENT-TYPE': ['application/json']])                                                     | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('{"json": true}'.bytes))                                             | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('{}'.bytes))                                                         | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('[]'.bytes))                                                         | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('[1,2,3]'.bytes))                                                    | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('"string"'.bytes))                                                   | 'application/json'
    new Request('Get', '', [:], [:], OptionalBody.body('<?xml version="1.0" encoding="UTF-8"?>\n<json>false</json>'.bytes)) | 'application/xml'
    new Request('Get', '', [:], [:], OptionalBody.body('<json>false</json>'.bytes))                                         | 'application/xml'
    new Request('Get', '', [:], [:], OptionalBody.body('this is not json'.bytes))                                           | 'text/plain'
    new Request('Get', '', [:], [:], OptionalBody.body('<html><body>this is also not json</body></html>'.bytes))            | 'text/html'
  }

  @Unroll
  def 'Pact charset'() {
    expect:
    request.charset() == charset

    where:
    request                                                                             | charset
    new Request('Get', '')                                                              | null
    new Request('Get', '', [:], ['Content-Type': ['text/html']])                        | Charset.defaultCharset()
    new Request('Get', '', [:], ['Content-Type': ['application/json; charset=UTF-16']]) | Charset.forName('UTF-16')
  }
}
