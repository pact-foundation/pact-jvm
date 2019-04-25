package au.com.dius.pact.provider

import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.Response
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
}
