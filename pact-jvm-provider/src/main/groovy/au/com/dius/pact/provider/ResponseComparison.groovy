package au.com.dius.pact.provider

import au.com.dius.pact.matchers.MatchingConfig
import au.com.dius.pact.model.BodyMismatch
import au.com.dius.pact.model.BodyTypeMismatch
import au.com.dius.pact.model.DiffConfig
import au.com.dius.pact.model.HeaderMismatch
import au.com.dius.pact.model.OptionalBody
@SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.Response
import au.com.dius.pact.model.ResponseMatching$
import au.com.dius.pact.model.ResponsePartMismatch
import au.com.dius.pact.model.StatusMismatch
import au.com.dius.pact.model.v3.messaging.Message
import difflib.Delta
import difflib.DiffUtils
import difflib.Patch
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import scala.None$
import scala.collection.JavaConverters$

/**
 * Utility class to compare responses
 */
class ResponseComparison {

    private static final String NL = '\n'

    Response expected
    Map actual
    int actualStatus
    Map actualHeaders
    String actualBody

  static compareResponse(Response response, Map actualResponse, int actualStatus, Map actualHeaders,
                             String actualBody) {
    def result = [:]
    def comparison = new ResponseComparison(expected: response, actual: actualResponse, actualStatus: actualStatus,
        actualHeaders: actualHeaders.collectEntries { k, v -> [k.toUpperCase(), v] }, actualBody: actualBody)
    def mismatches = JavaConverters$.MODULE$.seqAsJavaListConverter(
        ResponseMatching$.MODULE$.responseMismatches(response, new Response(actualStatus,
            actualHeaders, OptionalBody.body(actualBody)))).asJava()

    result.method = comparison.compareStatus(mismatches)
    result.headers = comparison.compareHeaders(mismatches)
    result.body = comparison.compareBody(mismatches)
    result
  }

  static compareMessage(Message message, OptionalBody actual) {
    def result = JavaConverters$.MODULE$.mutableMapAsJavaMapConverter(MatchingConfig.bodyMatchers()).asJava().find {
          message.contentType ==~ it.key
    }
    def mismatches = []
    def expected = message.asPactRequest()
    def actualMessage = new Response(200, ['Content-Type': message.contentType], actual)
    if (result) {
      mismatches = JavaConverters$.MODULE$.seqAsJavaListConverter(result.value.matchBody(expected,
            actualMessage, DiffConfig.apply(true, false))).asJava()
    } else {
      def expectedBody = message.contents.orElse('')
      if (!StringUtils.isEmpty(expectedBody) && StringUtils.isEmpty(actual)) {
          mismatches << BodyMismatch.apply(expectedBody, None$.MODULE$.get())
      } else if (actual.orElse('') != expectedBody) {
          mismatches << BodyMismatch(expectedBody, actual.orElse(''))
      }
    }

    new ResponseComparison(expected: expected, actual: [contentType: [mimeType: message.contentType]],
      actualBody: actual.orElse('')).compareBody(mismatches)
  }

  def compareStatus(List<ResponsePartMismatch> mismatches) {
    StatusMismatch statusMismatch = mismatches.find { it instanceof StatusMismatch }
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

    if (expected.headers != null) {
      def headerMismatchers = mismatches.findAll { it instanceof HeaderMismatch }.groupBy { it.headerKey }
      if (headerMismatchers.empty) {
          headerResult = expected.headers.keySet().collectEntries { [it, true] }
      } else {
        expected.headers.each { headerKey, value ->
              if (headerMismatchers[headerKey]) {
                  headerResult[headerKey] = headerMismatchers[headerKey].first().mismatch.get()
              } else {
                  headerResult[headerKey] = true
              }
        }
      }
    }

    headerResult
  }

  def compareBody(List<ResponsePartMismatch> mismatches) {
    def result = [:]

    BodyTypeMismatch bodyTypeMismatch = mismatches.find { it instanceof BodyTypeMismatch }
    if (bodyTypeMismatch) {
      result = [comparison: "Expected a response type of '${bodyTypeMismatch.expected()}' but the actual " +
          "type was '${bodyTypeMismatch.actual()}'"]
    } else if (mismatches.any { it instanceof BodyMismatch }) {
      result.comparison = mismatches.findAll { it instanceof BodyMismatch }.collectEntries {
          BodyMismatch bodyMismatch -> [bodyMismatch.path(), bodyMismatch.mismatch().defined ?
              bodyMismatch.mismatch().get() : 'mismatch']
      }

      String actualBodyString = ''
      if (actualBody) {
          if (actual.contentType.mimeType ==~ 'application/.*json') {
              def bodyMap = new JsonSlurper().parseText(actualBody)
              def bodyJson = JsonOutput.toJson(bodyMap)
              actualBodyString = JsonOutput.prettyPrint(bodyJson)
          } else {
              actualBodyString = actualBody
          }
      }

      String expectedBodyString = ''
      if (expected.body.present) {
          if (expected.jsonBody()) {
              expectedBodyString = JsonOutput.prettyPrint(expected.body.value)
          } else {
              expectedBodyString = expected.body.value
          }
      }

      def expectedLines = expectedBodyString.split(NL) as List
      def actualLines = actualBodyString.split(NL) as List
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
