package au.com.dius.pact.provider

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.BodyTypeMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.Matching
import au.com.dius.pact.core.matchers.MatchingConfig
import au.com.dius.pact.core.matchers.MatchingContext
import au.com.dius.pact.core.matchers.MetadataMismatch
import au.com.dius.pact.core.matchers.Mismatch
import au.com.dius.pact.core.matchers.ResponseMatching
import au.com.dius.pact.core.matchers.StatusMismatch
import au.com.dius.pact.core.matchers.generateDiff
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.core.model.V4Interaction
import au.com.dius.pact.core.model.isNullOrEmpty
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.messaging.MessageInteraction
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.Utils.sizeOf
import au.com.dius.pact.core.support.expressions.SystemPropertyResolver
import au.com.dius.pact.core.support.expressions.ValueResolver
import au.com.dius.pact.core.support.isNotEmpty
import au.com.dius.pact.core.support.jsonObject
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import mu.KLogging
import java.lang.Integer.max

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

  fun bodyResult(
    mismatches: List<Mismatch>,
    resolver: ValueResolver
  ): Result<BodyComparisonResult, BodyTypeMismatch> {
    val bodyTypeMismatch = mismatches.filterIsInstance<BodyTypeMismatch>().firstOrNull()
    return if (bodyTypeMismatch != null) {
      Err(bodyTypeMismatch)
    } else {
      val bodyMismatches = mismatches
        .filterIsInstance<BodyMismatch>()
        .groupBy { bm -> bm.path }

      val contentType = this.actualResponseContentType
      val expected = expectedBody.valueAsString()
      val actual = actualBody.orEmpty()
      val diff = when (val shouldIncludeDiff = shouldGenerateDiff(resolver, max(actual.length, expected.length))) {
        is Ok -> if (shouldIncludeDiff.value) {
          generateFullDiff(actual, contentType, expected, isJsonBody)
        } else {
          emptyList()
        }
        is Err -> {
          logger.warn { "Invalid value for property 'pact.verifier.generateDiff' - ${shouldIncludeDiff.error}" }
          emptyList()
        }
      }
      Ok(BodyComparisonResult(bodyMismatches, diff))
    }
  }

  companion object : KLogging() {
    private fun generateFullDiff(
      actual: String,
      contentType: ContentType,
      response: String,
      jsonBody: Boolean
    ): List<String> {
      var actualBodyString = ""
      if (actual.isNotEmpty()) {
        actualBodyString = if (contentType.isJson()) {
          Json.prettyPrint(actual)
        } else {
          actual
        }
      }

      var expectedBodyString = ""
      if (response.isNotEmpty()) {
        expectedBodyString = if (jsonBody) {
          Json.prettyPrint(response)
        } else {
          response
        }
      }

      return generateDiff(expectedBodyString, actualBodyString)
    }

    @JvmStatic
    fun shouldGenerateDiff(resolver: ValueResolver, length: Int): Result<Boolean, String> {
      val shouldIncludeDiff = resolver.resolveValue("pact.verifier.generateDiff", "NOT_SET")
      return when (val v = shouldIncludeDiff?.toLowerCase()) {
        "true", "not_set" -> Ok(true)
        "false" -> Ok(false)
        else -> if (v.isNotEmpty()) {
          when (val result = sizeOf(v!!)) {
            is Ok -> Ok(length <= result.value)
            is Err -> result
          }
        } else {
          Ok(false)
        }
      }
    }

    @JvmStatic
    fun compareResponse(response: IResponse, actualResponse: ProviderResponse): ComparisonResult {
      val actualResponseContentType = actualResponse.contentType
      val comparison = ResponseComparison(response.headers, response.body, response.asHttpPart().jsonBody(),
        actualResponseContentType, actualResponse.body)
      val mismatches = ResponseMatching.responseMismatches(response, Response(actualResponse.statusCode,
        actualResponse.headers.toMutableMap(), OptionalBody.body(actualResponse.body?.toByteArray(
        actualResponseContentType.asCharset()))))
      return ComparisonResult(comparison.statusResult(mismatches), comparison.headerResult(mismatches),
        comparison.bodyResult(mismatches, SystemPropertyResolver))
    }

    @JvmStatic
    @JvmOverloads
    fun compareMessage(
      message: MessageInteraction,
      actual: OptionalBody,
      metadata: Map<String, Any>? = null
    ): ComparisonResult {
      val bodyContext = when (message) {
        is V4Interaction.AsynchronousMessage ->
          MatchingContext(message.matchingRules.rulesForCategory("content"), true)
        else ->
          MatchingContext(message.matchingRules.rulesForCategory("body") ?: MatchingRuleCategory("body"), true)
      }
      val metadataContext = MatchingContext(message.matchingRules.rulesForCategory("metadata"), true)

      val bodyMismatches = compareMessageBody(message, actual, bodyContext)

      val metadataMismatches = when (metadata) {
        null -> emptyList()
        else -> Matching.compareMessageMetadata(message.metadata, metadata, metadataContext)
      }

      val messageContentType = message.getContentType().or(ContentType.TEXT_PLAIN)
      val responseComparison = ResponseComparison(
        mapOf("Content-Type" to listOf(messageContentType.toString())), message.contents,
        messageContentType.isJson(), messageContentType, actual.valueAsString())
      return ComparisonResult(bodyMismatches = responseComparison.bodyResult(bodyMismatches, SystemPropertyResolver),
        metadataMismatches = metadataMismatches.groupBy { it.key })
    }

    @JvmStatic
    fun compareMessageBody(
      message: MessageInteraction,
      actual: OptionalBody,
      context: MatchingContext
    ): MutableList<BodyMismatch> {
      val result = MatchingConfig.lookupBodyMatcher(message.getContentType().getBaseType())
      var bodyMismatches = mutableListOf<BodyMismatch>()
      if (result != null) {
        bodyMismatches = result.matchBody(message.contents, actual, context)
          .bodyResults.flatMap { it.result }.toMutableList()
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
