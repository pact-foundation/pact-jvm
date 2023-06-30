package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.padTo
import io.pact.plugins.jvm.core.InteractionContents
import mu.KLogging
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.net.WWWFormCodec

class FormPostContentMatcher : ContentMatcher {
  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult {
    val expectedBody = expected
    val actualBody = actual
    return when {
      expectedBody.isMissing() -> BodyMatchResult(null, emptyList())
      expectedBody.isPresent() && actualBody.isNotPresent() -> BodyMatchResult(null,
        listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expectedBody.orEmpty(),
              null, "Expected a form post body but was missing")))))
      expectedBody.isEmpty() && actualBody.isEmpty() -> BodyMatchResult(null, emptyList())
      else -> {
        val expectedParameters = WWWFormCodec.parse(expectedBody.valueAsString(), expected.contentType.asCharset())
        val actualParameters = WWWFormCodec.parse(actualBody.valueAsString(), actual.contentType.asCharset())
        BodyMatchResult(null, compareParameters(expectedParameters, actualParameters, context))
      }
    }
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Result<List<InteractionContents>, String> {
    return Result.Ok(listOf(InteractionContents("", OptionalBody.body(
        bodyConfig["body"].toString().toByteArray(),
        ContentType("application/x-www-form-urlencoded")
      ))))
  }

  @Suppress("LongMethod")
  private fun compareParameters(
    expectedParameters: List<NameValuePair>,
    actualParameters: List<NameValuePair>,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedMap = expectedParameters.groupBy { it.name }
    val actualMap = actualParameters.groupBy { it.name }
    val result = mutableListOf<BodyItemMatchResult>()
    expectedMap.forEach { entry ->
      if (actualMap.containsKey(entry.key)) {
        val actualParameterValues = actualMap[entry.key]!!
        val path = listOf("$", entry.key)
        if (context.matcherDefined(path)) {
          logger.debug { "Matcher defined for form post parameter '${entry.key}'" }
          entry.value.padTo(actualParameterValues.size).forEachIndexed { index, valuePair ->
            val childPath = path + index.toString()
            result.add(
              BodyItemMatchResult(
                childPath.joinToString("."),
                Matchers.domatch(context, childPath, valuePair.value,
                  actualParameterValues[index].value, BodyMismatchFactory)
              )
            )
          }
        } else {
          if (actualParameterValues.size > entry.value.size) {
            result.add(
              BodyItemMatchResult(
                path.joinToString("."), listOf(
                  BodyMismatch(
                    "${entry.key}=${entry.value.map { it.value }}",
                    "${entry.key}=${actualParameterValues.map { it.value }}",
                    "Expected form post parameter '${entry.key}' with ${entry.value.size} value(s) " +
                      "but received ${actualParameterValues.size} value(s)"
                  )
                )
              )
            )
          }
          entry.value.forEachIndexed { index, valuePair ->
            logger.debug { "No matcher defined for form post parameter '${entry.key}'[$index], using equality" }
            if (actualParameterValues.size <= index) {
              result.add(
                BodyItemMatchResult(
                  path.joinToString("."), listOf(
                    BodyMismatch(
                      "${entry.key}=${valuePair.value}", null,
                      "Expected form post parameter '${entry.key}'='${valuePair.value}' but was missing"
                    )
                  )
                )
              )
            } else if (valuePair.value != actualParameterValues[index].value) {
              val mismatch = if (entry.value.size == 1 && actualParameterValues.size == 1)
                "Expected form post parameter '${entry.key}' with value '${valuePair.value}'" +
                  " but was '${actualParameterValues[index].value}'"
              else
                "Expected form post parameter '${entry.key}'[$index] with value '${valuePair.value}'" +
                  " but was '${actualParameterValues[index].value}'"
              result.add(
                BodyItemMatchResult(
                  path.joinToString("."), listOf(
                    BodyMismatch(
                      "${entry.key}=${valuePair.value}",
                      "${entry.key}=${actualParameterValues[index].value}",
                      mismatch
                    )
                  )
                )
              )
            }
          }
        }
      } else {
        result.add(BodyItemMatchResult(entry.key, listOf(BodyMismatch(entry.key, null,
          "Expected form post parameter '${entry.key}' but was missing"))))
      }
    }

    if (!context.allowUnexpectedKeys) {
      actualMap.entries.forEach { entry ->
        if (!expectedMap.containsKey(entry.key)) {
          val values = entry.value.map { it.value }
          result.add(BodyItemMatchResult(entry.key, listOf(
            BodyMismatch(null, "${entry.key}=$values", "Received unexpected form post " +
              "parameter '${entry.key}'=${values.map { "'$it'" }}"))))
        }
      }
    }

    return result
  }

  companion object : KLogging()
}
