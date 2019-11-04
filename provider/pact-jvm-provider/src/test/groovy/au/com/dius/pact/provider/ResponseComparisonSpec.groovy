package au.com.dius.pact.provider

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import org.apache.http.entity.ContentType
import spock.lang.Specification

class ResponseComparisonSpec extends Specification {

  private Closure<Map> subject
  private Response response
  private actualResponse
  private int actualStatus
  private Map actualHeaders = ['A': ['B'], 'C': ['D'], 'Content-Type': ['application/json']]
  private actualBody

  void setup() {
    response = new Response(200, ['A': ['mismatch'], 'Content-Type': ['application/json']],
      OptionalBody.body('{"stuff": "is good"}'.bytes))
    actualStatus = 200
    actualBody = '{"stuff": "is good"}'
    actualResponse = [contentType: ContentType.APPLICATION_JSON]
    subject = { opts = [:] ->
      def status = opts.actualStatus ?: actualStatus
      def response = opts.response ?: response
      def actualHeaders = opts.actualHeaders ?: actualHeaders
      ResponseComparison.compareResponse(response, actualResponse, status, actualHeaders, actualBody)
    }
  }

  def 'compare the status should, well, compare the status'() {
    expect:
    subject().statusMismatch == null
    subject(actualStatus: 400).statusMismatch.description() == 'expected status of 200 but was 400'
  }

  def 'should not compare headers if there are no expected headers'() {
    given:
    response = new Response(200, [:], OptionalBody.body(''.bytes))

    expect:
    subject().headerMismatches.isEmpty()
  }

  def 'should only compare the expected headers'() {
    given:
    actualHeaders = ['A': ['B'], 'C': ['D']]
    def response = new Response(200, ['A': ['B']], OptionalBody.body(''.bytes))
    def response2 = new Response(200, ['A': ['D']], OptionalBody.body(''.bytes))

    expect:
    subject(actualHeaders: actualHeaders, response: response).headerMismatches.isEmpty()
    subject(actualHeaders: actualHeaders, response: response2).headerMismatches.A*.description() ==
      ['Expected header \'A\' to have value \'D\' but was \'B\'']
  }

  def 'ignores case in header comparisons'() {
    given:
    actualHeaders = ['A': ['B'], 'C': ['D']]
    response = new Response(200, ['a': ['B']], OptionalBody.body(''.bytes))

    expect:
    subject().headerMismatches.isEmpty()
  }

  def 'comparing bodies should fail with different content types'() {
    given:
    actualHeaders['Content-Type'] = ['text/plain']

    when:
    def result = subject().bodyMismatches

    then:
    result.isLeft()
    result.a.description() == 'Expected a response type of \'application/json\' but the actual type was \'text/plain\''
  }

  def 'comparing bodies should pass with the same content types and body contents'() {
    given:
    def result = subject().bodyMismatches

    expect:
    result.isRight()
    result.b.mismatches.isEmpty()
  }

  def 'comparing bodies should pass when the order of elements in the actual response is different'() {
    given:
    response = new Response(200, ['Content-Type': ['application/json']], OptionalBody.body(
            '{"moar_stuff": {"a": "is also good", "b": "is even better"}, "stuff": "is good"}'.bytes))
    actualBody = '{"stuff": "is good", "moar_stuff": {"b": "is even better", "a": "is also good"}}'
    def result = subject().bodyMismatches

    expect:
    result.isRight()
    result.b.mismatches.isEmpty()
  }

  def 'comparing bodies should show all the differences'() {
    given:
    actualBody = '{"stuff": "should make the test fail"}'
    def result = subject().bodyMismatches

    expect:
    result.isRight()
    result.b.mismatches.collectEntries { [ it.key, it.value*.description() ] } == [
      '$.stuff': ['Expected "is good" but received "should make the test fail"']
    ]
    result.b.diff[1] == '-  "stuff": "is good"'
    result.b.diff[2] == '+  "stuff": "should make the test fail"'
  }
}
