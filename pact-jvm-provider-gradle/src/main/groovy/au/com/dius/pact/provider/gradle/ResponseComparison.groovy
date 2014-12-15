package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.BodyMismatch
import au.com.dius.pact.model.BodyTypeMismatch
import au.com.dius.pact.model.HeaderMismatch
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.Response$
import au.com.dius.pact.model.ResponseMatching$
import au.com.dius.pact.model.ResponsePartMismatch
import au.com.dius.pact.model.StatusMismatch
import difflib.Delta
import difflib.DiffUtils
import difflib.Patch
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import scala.collection.JavaConverters$

class ResponseComparison {

  Response expected
  Map actual
  int actualStatus
  Map actualHeaders
  String actualBody

  static def compareResponse(Response response, Map actualResponse, int actualStatus, Map actualHeaders,
                             String actualBody) {
    def result = [:]
    def comparison = new ResponseComparison(expected: response, actual: actualResponse, actualStatus: actualStatus,
        actualHeaders: actualHeaders.collectEntries { k, v -> [k.toUpperCase(), v] }, actualBody: actualBody)
    def mismatches = JavaConverters$.MODULE$.seqAsJavaListConverter(
        ResponseMatching$.MODULE$.responseMismatches(response, Response$.MODULE$.apply(actualStatus,
            actualHeaders, actualBody, [:]))).asJava()

    result.method = comparison.compareStatus(mismatches)
    result.headers = comparison.compareHeaders(mismatches)
    result.body = comparison.compareBody(mismatches)
    result
  }

  def compareStatus(List<ResponsePartMismatch> mismatches) {
    StatusMismatch statusMismatch = mismatches.find{ it instanceof StatusMismatch }
    if (statusMismatch) {
      int expectedStatus = statusMismatch.expected()
      int actualStatus = statusMismatch.actual()
      try {
        assert expectedStatus == actualStatus
      } catch (PowerAssertionError e) {
        return e
      }
    }
    true
  }

  def compareHeaders(List<ResponsePartMismatch> mismatches) {
    Map headerResult = [:]

    HeaderMismatch headerMismatch = mismatches.find{ it instanceof HeaderMismatch }
    if (headerMismatch) {
      def headers = JavaConverters$.MODULE$.mapAsJavaMapConverter(headerMismatch.expected()).asJava()
      headers.each { headerKey, value ->
        try {
          assert actualHeaders[headerKey.toUpperCase()] == value
          headerResult[headerKey] = true
        } catch (PowerAssertionError e) {
          headerResult[headerKey] = e
        }
      }
    } else if (expected.headers().defined) {
      headerResult = JavaConverters$.MODULE$.mapAsJavaMapConverter(expected.headers().get()).asJava()
        .keySet().collectEntries{ [ it, true ] }
    }

    headerResult
  }

  def compareBody(List<ResponsePartMismatch> mismatches) {
    def result = [:]

    BodyTypeMismatch bodyTypeMismatch = mismatches.find{ it instanceof BodyTypeMismatch }
    if (bodyTypeMismatch) {
      result = [comparison: "Expected a response type of '${bodyTypeMismatch.expected()}' but the actual type was '${bodyTypeMismatch.actual()}'"]
    } else if (mismatches.any{ it instanceof BodyMismatch }) {
      result.comparison = mismatches.findAll{ it instanceof BodyMismatch }.collectEntries { BodyMismatch bodyMismatch ->
        [bodyMismatch.path(), bodyMismatch.mismatch().defined ? bodyMismatch.mismatch().get() : "mismatch"]
      }

      String actualBodyString = ''
      if (actualBody) {
          if (actual.contentType.mimeType ==~ 'application/.*json') {
              actualBodyString = new JsonBuilder(new JsonSlurper().parseText(actualBody)).toPrettyString()
          } else {
              actualBodyString = actualBody.toString()
          }
      }

      String expectedBodyString = ''
      if (expected.body().defined) {
          if (expected.jsonBody()) {
              expectedBodyString = new JsonBuilder(new JsonSlurper().parseText(expected.body().get())).toPrettyString()
          } else {
              expectedBodyString = expected.body().get()
          }
      }

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

      result.diff = diff
    }

    result
  }
}
