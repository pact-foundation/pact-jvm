package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.Mismatch
import au.com.dius.pact.core.matchers.ResponseMatching
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.matchers.generateDiff
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.orElse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import mu.KLogging
import org.apache.http.entity.ContentType

/**
 * Utility class to compare responses
 */
class ResponseComparison(
  val expected: Response,
  val actual: Map<String, Any>,
  val actualStatus: Int,
  val actualHeaders: Map<String, String>,
  val actualBody: String?
) {

  fun compareStatus(mismatches: List<Mismatch>): String? {
    val statusMismatch = mismatches.find { it is StatusMismatch } as StatusMismatch?
    if (statusMismatch != null) {
      val expectedStatus = statusMismatch.expected
      val actualStatus = statusMismatch.actual
      return "expected status of $expectedStatus but was $actualStatus"
    }
    return null
  }

  fun compareHeaders(mismatches: List<Mismatch>): Map<String, String?> {
    var headerResult = mutableMapOf<String, String?>()

    if (expected.headers != null) {
      val headerMismatchers = mismatches.filter { it is HeaderMismatch }
        .map { it as HeaderMismatch }
        .groupBy { it.headerKey }
      if (headerMismatchers.isEmpty()) {
          headerResult = expected.headers.orEmpty().mapValues { null }.toMutableMap()
      } else {
        expected.headers.orEmpty().forEach { headerKey, _ ->
          if (headerMismatchers[headerKey]!!.isNotEmpty()) {
              headerResult[headerKey] = headerMismatchers[headerKey]!!.first().mismatch
          } else {
              headerResult[headerKey] = null
          }
        }
      }
    }

    return headerResult
  }

  fun compareBody(mismatches: List<Mismatch>): Map<String, Any> {
    val result = mutableMapOf<String, Any>()

    val bodyTypeMismatch = mismatches.find { it is BodyTypeMismatch } as BodyTypeMismatch?
    if (bodyTypeMismatch != null) {
      result["comparison"] = "Expected a response type of '${bodyTypeMismatch.expected}' but the actual " +
          "type was '${bodyTypeMismatch.actual}'"
    } else if (mismatches.any { it is BodyMismatch }) {
      result["comparison"] = mismatches
        .filter { it is BodyMismatch }
        .map { it as BodyMismatch }
        .groupBy { bm -> bm.path }
        .entries
        .associate { (path, m) ->
          path to m.map { bm -> mapOf("mismatch" to (bm.mismatch ?: "mismatch"), "diff" to bm.diff.orEmpty()) }
        }

      val contentType = this.actual["contentType"] as ContentType
      result["diff"] = generateFullDiff(actualBody.orEmpty(), contentType.mimeType.toString(),
        expected.body.orElse(""), expected.jsonBody())
    }

    return result
  }

  companion object : KLogging() {

    private fun generateFullDiff(actual: String, mimeType: String, response: String, jsonBody: Boolean): List<String> {
      var actualBodyString = ""
      if (actual.isNotEmpty()) {
        actualBodyString = if (mimeType.matches(Regex("application/.*json"))) {
          val bodyMap = JsonSlurper().parseText(actual)
          val bodyJson = JsonOutput.toJson(bodyMap)
          JsonOutput.prettyPrint(bodyJson)
        } else {
          actual
        }
      }

      var expectedBodyString = ""
      if (response.isNotEmpty()) {
        expectedBodyString = if (jsonBody) {
          JsonOutput.prettyPrint(response)
        } else {
          response
        }
      }

      return generateDiff(expectedBodyString, actualBodyString)
    }

    @JvmStatic
    fun compareResponse(
      response: Response,
      actualResponse: Map<String, Any>,
      actualStatus: Int,
      actualHeaders: Map<String, String>,
      actualBody: String?
    ): Map<String, Any?> {
      val result = mutableMapOf<String, Any?>()
      val comparison = ResponseComparison(response, actualResponse, actualStatus,
        actualHeaders.mapKeys { it.key.toUpperCase() }, actualBody)
      val mismatches = ResponseMatching.responseMismatches(response, Response(actualStatus,
        actualHeaders, OptionalBody.body(actualBody)), true)

      result["method"] = comparison.compareStatus(mismatches)
      result["headers"] = comparison.compareHeaders(mismatches)
      result["body"] = comparison.compareBody(mismatches)

      return result
    }

    @JvmStatic
    fun compareMessage(message: Message, actual: OptionalBody): Map<String, Any> {
      val result = MatchingConfig.lookupBodyMatcher(message.contentType)
      var mismatches = mutableListOf<BodyMismatch>()
      val expected = message.asPactRequest()
      val actualMessage = Response(200, mapOf("Content-Type" to message.contentType), actual)
      if (result != null) {
        mismatches = result.matchBody(expected, actualMessage, true).toMutableList()
      } else {
        val expectedBody = message.contents.orElse("")
        if (expectedBody.isNotEmpty() && actual.value.isNullOrEmpty()) {
          mismatches.add(BodyMismatch(expectedBody, null))
        } else if (actual.orElse("") != expectedBody) {
          mismatches.add(BodyMismatch(expectedBody, actual.orElse("")))
        }
      }

      return ResponseComparison(expected as Response, mapOf("contentType" to ContentType.parse(message.contentType)),
        200, emptyMap(), actual.orElse("")).compareBody(mismatches)
    }
  }
}
