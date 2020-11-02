package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import mu.KLogging
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils

class FormPostBodyMatcher : BodyMatcher {
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
        val expectedParameters =
          URLEncodedUtils.parse(expectedBody.valueAsString(), expected.contentType.asCharset(), '&')
        val actualParameters =
        URLEncodedUtils.parse(actualBody.valueAsString(), actual.contentType.asCharset(), '&')
        BodyMatchResult(null, compareParameters(expectedParameters, actualParameters, context))
      }
    }
  }

  private fun compareParameters(
    expectedParameters: List<NameValuePair>,
    actualParameters: List<NameValuePair>,
    context: MatchingContext
  ): List<BodyItemMatchResult> {
    val expectedMap = expectedParameters.groupBy { it.name }
    val actualMap = actualParameters.groupBy { it.name }
    val result = mutableListOf<BodyItemMatchResult>()
    expectedMap.forEach {
      if (actualMap.containsKey(it.key)) {
        it.value.forEachIndexed { index, valuePair ->
          val path = listOf("$", it.key, index.toString())
          if (context.matcherDefined(path)) {
            logger.debug { "Matcher defined for form post parameter '${it.key}'[$index]" }
            result.add(
              BodyItemMatchResult(path.joinToString("."),
                Matchers.domatch(context, path, valuePair.value,
                  actualMap[it.key]!![index].value, BodyMismatchFactory)))
          } else {
            logger.debug { "No matcher defined for form post parameter '${it.key}'[$index], using equality" }
            val actualValues = actualMap[it.key]!!
            if (actualValues.size <= index) {
              result.add(BodyItemMatchResult(path.joinToString("."), listOf(
                BodyMismatch("${it.key}=${valuePair.value}", null,
                "Expected form post parameter '${it.key}'='${valuePair.value}' but was missing"))))
            } else if (valuePair.value != actualValues[index].value) {
              result.add(BodyItemMatchResult(path.joinToString("."), listOf(
                BodyMismatch("${it.key}=${valuePair.value}",
                "${it.key}=${actualValues[index].value}", "Expected form post parameter " +
                "'${it.key}'[$index] with value '${valuePair.value}' but was '${actualValues[index].value}'"))))
            }
          }
        }
      } else {
        result.add(BodyItemMatchResult(it.key, listOf(BodyMismatch(it.key, null,
          "Expected form post parameter '${it.key}' but was missing"))))
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
