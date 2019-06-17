package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import spock.lang.Specification

class RequestSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': ['': [matchers: [ [match: 'type'] ] ] ]
      ]
    ]

    when:
    def request = Request.fromJson(Json.INSTANCE.toJson(json).asJsonObject)

    then:
    !request.matchingRules.empty
    request.matchingRules.hasCategory('stuff')
  }

  def 'fromMap sets defaults for attributes missing from the map'() {
    expect:
    request.method == 'GET'
    request.path == '/'
    request.query.isEmpty()
    request.headers.isEmpty()
    request.body.isMissing()
    request.matchingRules.empty
    request.generators.empty

    where:
    request = Request.fromJson(Json.INSTANCE.toJson([:]).asJsonObject)
  }

  def 'detects multipart file uploads based on the content type'() {
    expect:
    new Request(headers: ['Content-Type': [contentType]]).isMultipartFileUpload() == multipartFileUpload

    where:

    contentType                                    | multipartFileUpload
    'multipart/form-data'                          | true
    'text/plain'                                   | false
    'multipart/form-data; boundary=boundaryMarker' | true
    'multipart/form-data;boundary=boundaryMarker'  | true
    'MULTIPART/FORM-DATA; boundary=boundaryMarker' | true
  }
}
