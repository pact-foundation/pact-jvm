package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.provider.ResponseComparison
import org.apache.http.entity.ContentType
import org.junit.Before
import org.junit.Test

@SuppressWarnings('ChainedTest')
class ResponseComparisonTest {

  private Closure<Map> testSubject
  private Response response
  private actualResponse
  private int actualStatus
  private Map actualHeaders = ['A': ['B'], 'C': ['D'], 'Content-Type': ['application/json']]
  private actualBody

  @Before
  void setup() {
    response = new Response(200, ['A': ['mismatch'], 'Content-Type': ['application/json']],
      OptionalBody.body('{"stuff": "is good"}'.bytes))
    actualStatus = 200
    actualBody = '{"stuff": "is good"}'
    actualResponse = [contentType: ContentType.APPLICATION_JSON]
    testSubject = {
      ResponseComparison.compareResponse(response, actualResponse, actualStatus, actualHeaders, actualBody)
    }
  }

  @Test
  void 'compare the status should, well, compare the status'() {
    assert testSubject().method == null
    actualStatus = 400
    assert testSubject().method == 'expected status of 200 but was 400'
  }

  @Test
  void 'should not compare headers if there are no expected headers'() {
    response = new Response(200, [:], OptionalBody.body(''.bytes))
    assert testSubject().headers == [:]
  }

  @Test
  void 'should only compare the expected headers'() {
    actualHeaders = ['A': ['B'], 'C': ['D']]
    response = new Response(200, ['A': ['B']], OptionalBody.body(''.bytes))
    assert testSubject().headers == ['A': null]
    response = new Response(200, ['A': ['D']], OptionalBody.body(''.bytes))
    assert testSubject().headers.A == 'Expected header \'A\' to have value \'D\' but was \'B\''
  }

  @Test
  void 'ignores case in header comparisons'() {
    actualHeaders = ['A': ['B'], 'C': ['D']]
    response = new Response(200, ['a': ['B']], OptionalBody.body(''.bytes))
    assert testSubject().headers == ['a': null]
  }

  @Test
  void 'comparing bodies should fail with different content types'() {
    actualHeaders['Content-Type'] = ['text/plain']
    assert testSubject().body == [comparison:
      'Expected a response type of \'application/json\' but the actual type was \'text/plain\'']
  }

  @Test
  void 'comparing bodies should pass with the same content types and body contents'() {
    assert testSubject().body == [:]
  }

  @Test
  void 'comparing bodies should pass when the order of elements in the actual response is different'() {
    response = new Response(200, ['Content-Type': ['application/json']], OptionalBody.body(
            '{"moar_stuff": {"a": "is also good", "b": "is even better"}, "stuff": "is good"}'.bytes))
    actualBody = '{"stuff": "is good", "moar_stuff": {"b": "is even better", "a": "is also good"}}'
    assert testSubject().body == [:]
  }

  @Test
  void 'comparing bodies should show all the differences'() {
    actualBody = '{"stuff": "should make the test fail"}'
    def result = testSubject().body
    assert result.comparison == [
      '$.stuff': [[mismatch: 'Expected "is good" but received "should make the test fail"', diff: '']]
    ]
    assert result.diff[1] == '-    "stuff": "is good"'
    assert result.diff[2] == '+    "stuff": "should make the test fail"'
  }
}
