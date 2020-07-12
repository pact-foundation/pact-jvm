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
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.isNullOrEmpty
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.jsonObject
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.gson.JsonParser
import mu.KLogging

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
  val bodyMismatches: Result<BodyComparisonResult, BodyTypeMismatch> = Ok(BodyComparisonResult()),
  val metadataMismatches: Map<String, List<MetadataMismatch>> = emptyMap()
)

/**
 * Utility class to compare responses
 */
class ResponseComparison(
  private val expectedHeaders: Map<String, List<String>>,
  private val expectedBody: OptionalBody,
  private val isJsonBody: Boolean,
  private val actualResponseContentType: ContentType,
  private val actualBody: String?
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

  fun bodyResult(mismatches: List<Mismatch>): Result<BodyComparisonResult, BodyTypeMismatch> {
    val bodyTypeMismatch = mismatches.filterIsInstance<BodyTypeMismatch>().firstOrNull()
    return if (bodyTypeMismatch != null) {
      Err(bodyTypeMismatch)
    } else {
      val bodyMismatches = mismatches
        .filterIsInstance<BodyMismatch>()
        .groupBy { bm -> bm.path }

      val contentType = this.actualResponseContentType
      val diff = generateFullDiff(actualBody.orEmpty(), contentType, expectedBody.valueAsString(), isJsonBody)
      Ok(BodyComparisonResult(bodyMismatches, diff))
    }
  }

  companion object : KLogging() {

    private fun generateFullDiff(actual: String, contentType: ContentType, response: String, jsonBody: Boolean): List<String> {
      var actualBodyString = ""
      if (actual.isNotEmpty()) {
        actualBodyString = if (contentType.isJson()) {
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
    fun compareResponse(response: Response, actualResponse: ProviderResponse): ComparisonResult {
      val actualResponseContentType = actualResponse.contentType
      val comparison = ResponseComparison(response.headers, response.body, response.jsonBody(),
        actualResponseContentType, actualResponse.body)
      val mismatches = ResponseMatching.responseMismatches(response, Response(actualResponse.statusCode,
        actualResponse.headers.toMutableMap(), OptionalBody.body(actualResponse.body?.toByteArray(
        actualResponseContentType.asCharset()))), true)
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

      val messageContentType = message.getContentType().or(ContentType.TEXT_PLAIN)
      val responseComparison = ResponseComparison(
        mapOf("Content-Type" to listOf(messageContentType.toString())), message.contents,
        messageContentType.isJson(), messageContentType, actual.valueAsString())
      return ComparisonResult(bodyMismatches = responseComparison.bodyResult(bodyMismatches),
        metadataMismatches = metadataMismatches.groupBy { it.key })
    }

    @JvmStatic
    private fun compareMessageBody(message: Message, actual: OptionalBody): MutableList<BodyMismatch> {
      val result = MatchingConfig.lookupBodyMatcher(message.getContentType().getBaseType())
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
