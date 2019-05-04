package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.provider.ResponseComparison
import org.apache.http.entity.ContentType
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('PrivateFieldCouldBeFinal')
class ResponseComparisonSpec extends Specification {

  private Closure<Map> comparison
  private Response response
  private actualResponse
  private int actualStatus
  private Map actualHeaders = ['A': ['B'], 'C': ['D'], 'Content-Type': ['application/json']]
  private actualBody

  def setup() {
    response = new Response(200, ['A': ['mismatch'], 'Content-Type': ['application/json']],
      OptionalBody.body('{"stuff": "is good"}'.bytes))
    actualStatus = 200
    actualBody = '{"stuff": "is good"}'
    actualResponse = [contentType: ContentType.APPLICATION_JSON]
    comparison = {
      ResponseComparison.compareResponse(response, actualResponse, actualStatus, actualHeaders, actualBody)
    }
  }

  def 'comparing bodies should fail with different content types'() {
    given:
    actualHeaders['Content-Type'] = ['text/plain']

    expect:
    comparison().body == [
      comparison: 'Expected a response type of \'application/json\' but the actual type was \'text/plain\''
    ]
  }

  def 'comparing bodies should pass with the same content types and body contents'() {
    expect:
    comparison().body == [:]
  }

  def 'comparing bodies should pass when the order of elements in the actual response is different'() {
    given:
    response = new Response(200, ['Content-Type': ['application/json']], OptionalBody.body(
      '{"moar_stuff": {"a": "is also good", "b": "is even better"}, "stuff": "is good"}'.bytes))
    actualBody = '{"stuff": "is good", "moar_stuff": {"b": "is even better", "a": "is also good"}}'

    expect:
    comparison().body == [:]
  }

  def 'comparing bodies should show all the differences'() {
    given:
    actualBody = '{"stuff": "should make the test fail"}'
    def result = comparison().body

    expect:
    result.comparison == [
      '$.stuff': [[mismatch: 'Expected "is good" but received "should make the test fail"', diff: '']]
    ]
    result.diff[1] == '-    "stuff": "is good"'
    result.diff[2] == '+    "stuff": "should make the test fail"'
  }

  @Unroll
  def 'when comparing message bodies, handles content type #contentType'() {
    given:
    Message expectedMessage = new Message(metaData: [contentType: contentType],
      contents: OptionalBody.body(expected.bytes))
    OptionalBody actualMessage = OptionalBody.body(actual.bytes)

    expect:
    ResponseComparison.compareMessageBody(expectedMessage, actualMessage, expectedMessage.asPactRequest()).empty

    where:

    contentType                                | expected                    | actual
    'application/json'                         | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'
    'application/json;charset=UTF-8'           | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'
    'application/json; charset\u003dUTF-8'     | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'
    'application/hal+json; charset\u003dUTF-8' | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'
    'text/plain'                               | '{"a": 100.0, "b": "test"}' | '{"a": 100.0, "b": "test"}'
    'application/octet-stream;charset=UTF-8'   | '{"a": 100.0, "b": "test"}' | '{"a": 100.0, "b": "test"}'
    'application/octet-stream'                 | '{"a": 100.0, "b": "test"}' | '{"a": 100.0, "b": "test"}'
    ''                                         | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'
    null                                       | '{"a": 100.0, "b": "test"}' | '{"a":100.0,"b":"test"}'

  }

}
