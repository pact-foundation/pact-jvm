package au.com.dius.pact.model

import spock.lang.Specification
import spock.lang.Unroll

class HttpPartSpec extends Specification {

  @SuppressWarnings('LineLength')
  @Unroll
  def 'Pact mimeType'() {
    expect:
    request.mimeType() == mimeType

    where:
    request                                                                                          | mimeType
    new Request('Get', '')                                                                           | 'text/plain'
    new Request('Get', '', null, ['Content-Type': 'text/html'])                                      | 'text/html'
    new Request('Get', '', null, ['Content-Type': 'application/json; charset=UTF-8'])                | 'application/json'
    new Request('Get', '', null, ['content-type': 'application/json'])                               | 'application/json'
    new Request('Get', '', null, ['CONTENT-TYPE': 'application/json'])                               | 'application/json'
    new Request('Get', '', null, null, '{"json": true}')                                             | 'application/json'
    new Request('Get', '', null, null, '{}')                                                         | 'application/json'
    new Request('Get', '', null, null, '[]')                                                         | 'application/json'
    new Request('Get', '', null, null, '[1,2,3]')                                                    | 'application/json'
    new Request('Get', '', null, null, '"string"')                                                   | 'application/json'
    new Request('Get', '', null, null, '<?xml version="1.0" encoding="UTF-8"?>\n<json>false</json>') | 'application/xml'
    new Request('Get', '', null, null, '<json>false</json>')                                         | 'application/xml'
    new Request('Get', '', null, null, 'this is not json')                                           | 'text/plain'
    new Request('Get', '', null, null, '<html><body>this is also not json</body></html>')            | 'text/html'
  }
}
