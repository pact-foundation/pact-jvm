package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Response
import difflib.Delta
import difflib.DiffUtils
import difflib.Patch
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import scala.collection.JavaConverters$

class ResponseComparison {

  Response expected
  int actualStatus
  Map actualHeaders
  def actualBody

  static def compareResponse(Response response, int actualStatus, Map actualHeaders, def actualBody) {
    def result = [:]
    def comparison = new ResponseComparison(expected: response, actualStatus: actualStatus,
        actualHeaders: actualHeaders.collectEntries { k, v -> [k.toUpperCase(), v] }, actualBody: actualBody)
    result.method = comparison.compareMethod()
    result.headers = comparison.compareHeaders()
    result.body = comparison.compareBody()
    result
  }

  def compareMethod() {
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
        def headers = []
        if (JavaConverters$.MODULE$.metaClass.respondsTo('asJavaMapConverter')) {
            headers = JavaConverters$.MODULE$.asJavaMapConverter(expected.headers().get()).asJava()
        } else {
            headers = JavaConverters$.MODULE$.mapAsJavaMapConverter(expected.headers().get()).asJava()
        }
        headers.each { headerKey, value ->
        try {
          assert actualHeaders[headerKey.toUpperCase()] == value
          headerResult[headerKey] = true
        } catch (PowerAssertionError e) {
          headerResult[headerKey] = e
        }
      }
    }

    headerResult
  }

  def compareBody() {
      def result = [:]
      def expectedBody = expected.body().defined ? new JsonSlurper().parseText(expected.bodyString().get()) : [:]
      def compareResult = BodyComparison.compare('/', expectedBody, actualBody)
      if (!compareResult.isEmpty()) {
          String actualBodyString = new JsonBuilder(actualBody).toPrettyString()
          String expectedBodyString = new JsonBuilder(expectedBody).toPrettyString()
          def expectedLines = expectedBodyString.split('\n') as List
          def actualLines = actualBodyString.split('\n') as List
          Patch<String> patch = DiffUtils.diff(expectedLines, actualLines)

          def diff = []

          patch.deltas.each { Delta<String> delta ->
              diff << "@${delta.original.position}"
              if (delta.original.position > 1) {
                  diff << expectedLines[delta.original.position - 1]
              }

              delta.original.lines.each {
                  diff << "-$it"
              }
              delta.revised.lines.each {
                  diff << "+$it"
              }

              int pos = delta.original.position + delta.original.lines.size()
              if (pos < expectedLines.size()) {
                  diff << expectedLines[pos]
              }

              diff << ''
          }

          result = [comparison: compareResult, diff: diff]
      }
      result
  }
}
