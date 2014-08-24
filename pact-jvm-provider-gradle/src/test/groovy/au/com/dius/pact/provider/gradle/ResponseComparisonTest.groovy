package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Response
import au.com.dius.pact.model.Response$
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Before
import org.junit.Test

class ResponseComparisonTest {

  Closure<ResponseComparison> testSubject
  Response response
  HttpResponse actualResponse
  int actualStatus
  Map actualHeaders = ['A': 'B', 'C': 'D', 'Content-Type': 'application/json']
  def actualBody

  @Before
  void setup() {
    response = Response$.MODULE$.apply(200, ['Content-Type': 'application/json'], '{"stuff": "is good"}', null)
    actualStatus = 200
    actualBody = [
      stuff: 'is good'
    ]
    def contentTypeHeader = [getValue: { actualHeaders['Content-Type'] }] as Header
    def entity = [getContentType: { contentTypeHeader }] as HttpEntity
    actualResponse = [getEntity: { entity }] as HttpResponse
    testSubject = { new ResponseComparison(expected: response, actual: actualResponse, actualStatus: actualStatus,
      actualHeaders: actualHeaders, actualBody: actualBody) }
  }

  @Test
  void 'compare the method should, well, compare the method'() {
    assert testSubject().compareMethod() == true
    actualStatus = 400
    assert testSubject().compareMethod() instanceof PowerAssertionError
  }

  @Test
  void 'should not compare headers if there are no expected headers'() {
    response = Response$.MODULE$.apply(200, [:], "", null)
    assert testSubject().compareHeaders() == [:]
  }

  @Test
  void 'should only compare the expected headers'() {
    actualHeaders = ['A': 'B', 'C': 'D']
    response = Response$.MODULE$.apply(200, ['A': 'B'], "", null)
    assert testSubject().compareHeaders() == ['A': true]
    response = Response$.MODULE$.apply(200, ['A': 'D'], "", null)
    assert testSubject().compareHeaders().A instanceof PowerAssertionError
  }

  @Test
  void 'ignores case in header comparisons'() {
    actualHeaders = ['A': 'B', 'C': 'D']
    response = Response$.MODULE$.apply(200, ['a': 'B'], "", null)
    assert testSubject().compareHeaders() == ['a': true]
  }

  @Test
  void 'comparing bodies should fail with different content types'() {
    actualHeaders['Content-Type'] = 'text/plain'
    assert testSubject().compareBody() == [comparison: "Expected a response type of 'application/json' but the actual type was 'text/plain'"]
  }

  @Test
  void 'comparing bodies should pass with the same content types and body contents'() {
    assert testSubject().compareBody() == [:]
  }

  @Test
  void 'comparing bodies should show all the differences'() {
    actualBody = [
      stuff: 'should make the test fail'
    ]
    def result = testSubject().compareBody()
    assert result.comparison == ['/stuff/': "Expected 'is good' but received 'should make the test fail'"]
    assert result.diff[0] == '@1'
    assert result.diff[1] == '-    "stuff": "is good"'
    assert result.diff[2] == '+    "stuff": "should make the test fail"'
  }
}
