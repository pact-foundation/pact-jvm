package au.com.dius.pact.provider

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.isNullOrEmpty
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.Json
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonParser
import mu.KLogging
import java.nio.charset.Charset

data class BodyComparisonResult(
  val mismatches: Map<String, List<BodyMismatch>> = emptyMap(),
  val diff: List<String> = emptyList()
) {
  fun toJson() = jsonObject(
    "mismatches" to Json.toJson(mismatches.mapValues { entry -> entry.value.map { it.description() } }),
    "diff" to diff.joinToString("\n")
  )
}

data class ComparisonResult(
  val statusMismatch: StatusMismatch? = null,
  val headerMismatches: Map<String, List<HeaderMismatch>> = emptyMap(),
  val bodyMismatches: Either<BodyTypeMismatch, BodyComparisonResult> = Either.Right(BodyComparisonResult()),
  val metadataMismatches: Map<String, List<MetadataMismatch>> = emptyMap()
)

/**
 * Utility class to compare responses
 */
class ResponseComparison(
  val expectedHeaders: Map<String, List<String>>,
  val expectedBody: OptionalBody,
  val isJsonBody: Boolean,
  val actualResponseContentType: org.apache.http.entity.ContentType,
  val actualBody: String?
) {

  fun statusResult(mismatches: List<Mismatch>) = mismatches.filterIsInstance<StatusMismatch>().firstOrNull()

  fun headerResult(mismatches: List<Mismatch>): Map<String, List<HeaderMismatch>> {
    val headerMismatchers = mismatches.filterIsInstance<HeaderMismatch>()
      .groupBy { it.headerKey }
    return if (headerMismatchers.isEmpty()) {
      emptyMap()
    } else {
      expectedHeaders.entries.associate { (headerKey, _) ->
        headerKey to headerMismatchers[headerKey].orEmpty()
      }
    }
  }

  fun bodyResult(mismatches: List<Mismatch>): Either<BodyTypeMismatch, BodyComparisonResult> {
    val bodyTypeMismatch = mismatches.filterIsInstance<BodyTypeMismatch>().firstOrNull()
    return if (bodyTypeMismatch != null) {
      bodyTypeMismatch.left()
    } else {
      val bodyMismatches = mismatches
        .filterIsInstance<BodyMismatch>()
        .groupBy { bm -> bm.path }

      val contentType = this.actualResponseContentType
      val diff = generateFullDiff(actualBody.orEmpty(), contentType.mimeType.toString(),
        expectedBody.valueAsString(), isJsonBody)
      BodyComparisonResult(bodyMismatches, diff).right()
    }
  }

  companion object : KLogging() {

    private fun generateFullDiff(actual: String, mimeType: String, response: String, jsonBody: Boolean): List<String> {
      var actualBodyString = ""
      if (actual.isNotEmpty()) {
        actualBodyString = if (mimeType.matches(Regex("application/.*json"))) {
          Json.gsonPretty.toJson(JsonParser.parseString(actual))
        } else {
          actual
        }
      }

      var expectedBodyString = ""
      if (response.isNotEmpty()) {
        expectedBodyString = if (jsonBody) {
          Json.gsonPretty.toJson(JsonParser.parseString(response))
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
    ): ComparisonResult {
      val actualResponseContentType = actualResponse["contentType"] as org.apache.http.entity.ContentType
      val comparison = ResponseComparison(response.headers, response.body, response.jsonBody(),
        actualResponseContentType, actualBody)
      val mismatches = ResponseMatching.responseMismatches(response, Response(actualStatus,
        actualHeaders.toMutableMap(), OptionalBody.body(actualBody?.toByteArray(
        actualResponseContentType.charset ?: Charset.defaultCharset()))), true)
      return ComparisonResult(comparison.statusResult(mismatches), comparison.headerResult(mismatches),
        comparison.bodyResult(mismatches))
    }

    @JvmStatic
    @JvmOverloads
    fun compareMessage(message: Message, actual: OptionalBody, metadata: Map<String, Any>? = null): ComparisonResult {
      val bodyMismatches = compareMessageBody(message, actual)

      val metadataMismatches = when (metadata) {
        null -> emptyList()
        else -> Matching.compareMessageMetadata(message.metaData, metadata, message.matchingRules)
      }

      val messageContentType = message.getContentType()
      val contentType = if (messageContentType.isNullOrEmpty()) Message.TEXT else messageContentType
      val responseComparison = ResponseComparison(
        mapOf("Content-Type" to listOf(contentType)), message.contents,
        contentType == ContentType.JSON.contentType,
        org.apache.http.entity.ContentType.parse(contentType), actual.valueAsString())
      return ComparisonResult(bodyMismatches = responseComparison.bodyResult(bodyMismatches),
        metadataMismatches = metadataMismatches.groupBy { it.key })
    }

    @JvmStatic
    private fun compareMessageBody(message: Message, actual: OptionalBody): MutableList<BodyMismatch> {
      val result = MatchingConfig.lookupBodyMatcher(message.getContentType().orEmpty())
      var bodyMismatches = mutableListOf<BodyMismatch>()
      if (result != null) {
        bodyMismatches = result.matchBody(message.contents, actual, true, message.matchingRules)
          .toMutableList()
      } else {
        val expectedBody = message.contents.valueAsString()
        if (expectedBody.isNotEmpty() && actual.isNullOrEmpty()) {
          bodyMismatches.add(BodyMismatch(expectedBody, null, "Expected body '$expectedBody' but was missing"))
        } else if (expectedBody.isNotEmpty() && actual.valueAsString() != expectedBody) {
          bodyMismatches.add(BodyMismatch(expectedBody, actual.valueAsString(),
            "Actual body '${actual.valueAsString()}' is not equal to the expected body '$expectedBody'"))
        }
      }
      return bodyMismatches
    }
  }
}
