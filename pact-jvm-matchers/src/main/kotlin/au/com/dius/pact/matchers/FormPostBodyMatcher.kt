package au.com.dius.pact.matchers

import au.com.dius.pact.model.HttpPart
import au.com.dius.pact.model.isEmpty
import au.com.dius.pact.model.isMissing
import au.com.dius.pact.model.isNotPresent
import au.com.dius.pact.model.isPresent
import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.orElse
import mu.KLogging
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils

class FormPostBodyMatcher : BodyMatcher {
  override fun matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List<BodyMismatch> {
    val expectedBody = expected.body
    val actualBody = actual.body
    return when {
      expectedBody.isMissing() -> emptyList()
      expectedBody.isPresent() && actualBody.isNotPresent() -> listOf(BodyMismatch(expectedBody.orElse(""),
        null, "Expected a form post body but was missing"))
      expectedBody.isEmpty() && actualBody.isEmpty() -> emptyList()
      else -> {
        val expectedParameters = URLEncodedUtils.parse(expectedBody.orElse(""), expected.charset(), '&')
        val actualParameters = URLEncodedUtils.parse(actualBody.orElse(""), actual.charset(), '&')
        compareParameters(expectedParameters, actualParameters, expected.matchingRules, allowUnexpectedKeys)
      }
    }
  }

  private fun compareParameters(expectedParameters: List<NameValuePair>,
                                actualParameters: List<NameValuePair>,
                                matchingRules: MatchingRules?,
                                allowUnexpectedKeys: Boolean): List<BodyMismatch> {
    val expectedMap = expectedParameters.groupBy { it.name }
    val actualMap = actualParameters.groupBy { it.name }
    val result = mutableListOf<BodyMismatch>()
    expectedMap.forEach {
      if (actualMap.containsKey(it.key)) {
        it.value.forEachIndexed { index, valuePair ->
          val path = listOf("$", it.key, index.toString())
          if (matchingRules != null && Matchers.matcherDefined("body", path, matchingRules)) {
            logger.debug { "Matcher defined for form post parameter '${it.key}'[$index]" }
            result.addAll(Matchers.domatch(matchingRules, "body", path, valuePair.value, actualMap[it.key]!![index].value, BodyMismatchFactory))
          } else {
            logger.debug { "No matcher defined for form post parameter '${it.key}'[$index], using equality" }
            val actualValues = actualMap[it.key]!!
            if (actualValues.size <= index) {
              result.add(BodyMismatch("${it.key}=${valuePair.value}", null, "Expected form post parameter '${it.key}'='${valuePair.value}' but was missing"))
            } else if (valuePair.value != actualValues[index].value) {
              result.add(BodyMismatch("${it.key}=${valuePair.value}", "${it.key}=${actualValues[index].value}", "Expected form post parameter '${it.key}'[$index] with value '${valuePair.value}' but was '${actualValues[index].value}'"))
            }
          }
        }
      } else {
        result.add(BodyMismatch(it.key, null, "Expected form post parameter '${it.key}' but was missing"))
      }
    }

    if (!allowUnexpectedKeys) {
      actualMap.entries.forEach {
        if (!expectedMap.containsKey(it.key)) {
          val values = it.value.map { it.value }
          result.add(BodyMismatch(null, "${it.key}=$values", "Received unexpected form post parameter '${it.key}'=${values.map { "'$it'" }}"))
        }
      }
    }

    return result
  }

  companion object : KLogging()
}
