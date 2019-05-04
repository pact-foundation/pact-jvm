package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.Matching
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.Mismatch
import au.com.dius.pact.core.matchers.ResponseMatching
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.matchers.generateDiff
import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.isNullOrEmpty
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.valueAsString
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
  val actualHeaders: Map<String, List<String>>,
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
          if (headerMismatchers.containsKey(headerKey) && headerMismatchers[headerKey]!!.isNotEmpty()) {
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
        expected.body.valueAsString(), expected.jsonBody())
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
      actualHeaders: Map<String, List<String>>,
      actualBody: String?
    ): Map<String, Any?> {
      val result = mutableMapOf<String, Any?>()
      val comparison = ResponseComparison(response, actualResponse, actualStatus,
        actualHeaders.mapKeys { it.key.toUpperCase() }, actualBody)
      val mismatches = ResponseMatching.responseMismatches(response, Response(actualStatus,
        actualHeaders, OptionalBody.body(actualBody?.toByteArray())), true)

      result["method"] = comparison.compareStatus(mismatches)
      result["headers"] = comparison.compareHeaders(mismatches)
      result["body"] = comparison.compareBody(mismatches)

      return result
    }

    @JvmStatic
    @JvmOverloads
    fun compareMessage(message: Message, actual: OptionalBody, metadata: Map<String, Any>? = null): Map<String, Any?> {
      val expected = message.asPactRequest()
      val bodyMismatches = compareMessageBody(message, actual, expected)

      val metadataMismatches = when (metadata) {
        null -> emptyList()
        else -> Matching.compareMessageMetadata(message.metaData, metadata, message.matchingRules)
      }

      val responseComparison = ResponseComparison(expected as Response,
        mapOf("contentType" to ContentType.parse(message.contentType)), 200, emptyMap(), actual.valueAsString())
      val result = mutableMapOf<String, Any?>()
      result["body"] = responseComparison.compareBody(bodyMismatches)
      result["metadata"] = metadataResult(metadataMismatches)
      return result
    }

    private fun metadataResult(mismatches: List<MetadataMismatch>): Map<String, Any> {
      return if (mismatches.isNotEmpty()) {
        mismatches.groupBy { it.key }.mapValues { (_, value) ->
          value.joinToString(", ") { it.mismatch }
        }
      } else {
        emptyMap()
      }
    }

    @JvmStatic
    private fun compareMessageBody(message: Message, actual: OptionalBody, expected: HttpPart): MutableList<BodyMismatch> {
      val result = MatchingConfig.lookupBodyMatcher(message.parsedContentType?.mimeType.orEmpty())
      var bodyMismatches = mutableListOf<BodyMismatch>()
      val actualMessage = Response(200, mapOf("Content-Type" to listOf(message.contentType)), actual)
      if (result != null) {
        bodyMismatches = result.matchBody(expected, actualMessage, true).toMutableList()
      } else {
        val expectedBody = message.contents.valueAsString()
        if (expectedBody.isNotEmpty() && actual.isNullOrEmpty()) {
          bodyMismatches.add(BodyMismatch(expectedBody, null))
        } else if (actual.valueAsString() != expectedBody) {
          bodyMismatches.add(BodyMismatch(expectedBody, actual.valueAsString()))
        }
      }
      return bodyMismatches
    }
  }
}
