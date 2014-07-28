package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Response
import org.apache.http.HttpResponse
import org.apache.http.Header
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import scala.collection.JavaConverters$

class ResponseComparison {

  Response expected
  HttpResponse actual

  static def compareResponse(Response response, HttpResponse actual) {
    def result = [:]
    def comparison = new ResponseComparison(expected: response, actual: actual)
    result.method = comparison.compareMethod()
    result.headers = comparison.compareHeaders()
    result.body = comparison.compareBody()
    result
  }

  def compareMethod() {
    int actualStatus = actual.statusLine.statusCode
    int expectedStatus = expected.status()
    try {
      assert actualStatus == expectedStatus
      return true
    } catch (PowerAssertionError e) {
      return e
    }
  }

  def compareHeaders() {
    Map headerResult = [:]

    if (expected.headers().defined) {
      JavaConverters$.MODULE$.asJavaMapConverter(expected.headers().get()).asJava().each { headerKey, value ->
        try {
          assert actual.containsHeader(headerKey)
          Header actualHeader = actual.getFirstHeader(headerKey)
          assert actualHeader.value == value
          headerResult[headerKey] = true
        } catch (PowerAssertionError e) {
          headerResult[headerKey] = e
        }
      }
    }

    headerResult
  }

  def compareBody() {
    def actualBody = actual.data?.text ?: ''
    def expectedBody = expected.body().defined ? expected.bodyString().get() : ''
    try {
      assert actualBody == expectedBody
      return true
    } catch (PowerAssertionError e) {
      return e
    }
  }
}
