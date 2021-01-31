package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import spock.lang.Issue
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
    def request = Request.fromJson(Json.INSTANCE.toJson(json).asObject())

    then:
    !request.matchingRules.empty
    request.matchingRules.hasCategory('stuff')
  }

  @SuppressWarnings('UnnecessaryGetter')
  def 'fromMap sets defaults for attributes missing from the map'() {
    expect:
    request.method == 'GET'
    request.path == '/'
    request.query.isEmpty()
    request.headers.isEmpty()
    request.body.missing
    request.matchingRules.empty
    request.generators.empty

    where:
    request = Request.fromJson(Json.INSTANCE.toJson([:]).asObject())
  }

  def 'detects multipart file uploads based on the content type'() {
    expect:
    new Request(headers: ['Content-Type': [contentType]]).multipartFileUpload == multipartFileUpload

    where:

    contentType                                    | multipartFileUpload
    'multipart/form-data'                          | true
    'text/plain'                                   | false
    'multipart/form-data; boundary=boundaryMarker' | true
    'multipart/form-data;boundary=boundaryMarker'  | true
    'MULTIPART/FORM-DATA; boundary=boundaryMarker' | true
  }

  def 'handles the cookie header'() {
    expect:
    new Request(headers: ['Cookie': ['test=12345; test2=abcd']]).cookie() == ['test=12345', 'test2=abcd']
  }

  def 'handles the cookie header with multiple values'() {
    expect:
    new Request(headers: ['Cookie': ['test=12345', 'test2=abcd; test3=xgfes']]).cookie() == [
      'test=12345', 'test2=abcd', 'test3=xgfes'
    ]
  }

  @Issue('#1288')
  def 'when loading from json, do not split header values'() {
    expect:
    Request.fromJson(Json.INSTANCE.toJson([
      headers: [
        'Expires': 'Sat, 27 Nov 1999 12:00:00 GMT',
        'Content-Type': 'application/json'
      ]
    ]).asObject()).headers == [
      Expires: ['Sat, 27 Nov 1999 12:00:00 GMT'],
      'Content-Type': ['application/json']
    ]
  }
}
