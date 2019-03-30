package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.HttpPart
import au.com.dius.pact.core.model.isEmpty
import au.com.dius.pact.core.model.isMissing
import au.com.dius.pact.core.model.isNull
import au.com.dius.pact.core.model.isPresent
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.valueAsString
import mu.KLogging

class PlainTextBodyMatcher : BodyMatcher {

  override fun matchBody(expected: HttpPart, actual: HttpPart, allowUnexpectedKeys: Boolean): List<BodyMismatch> {
    val expectedBody = expected.body
    val actualBody = actual.body
    return when {
      expectedBody.isMissing() -> emptyList()
      expectedBody.isNull() && actualBody.isPresent() -> listOf(
              BodyMismatch(null, actualBody!!.value, "Expected empty body but received '${actualBody.value}'"))
      expectedBody.isNull() -> emptyList()
      actualBody.isMissing() -> listOf(BodyMismatch(expectedBody!!.value, null,
              "Expected body '${expectedBody.value}' but was missing"))
      expectedBody.isEmpty() && actualBody.isEmpty() -> emptyList()
      else -> compareText(expectedBody.valueAsString(), actualBody.valueAsString(), expected.matchingRules)
    }
  }

  fun compareText(expected: String, actual: String, matchers: MatchingRules?): List<BodyMismatch> {
    val regexMatcher = matchers?.rulesForCategory("body")?.matchingRules?.get("$")
    val regex = regexMatcher?.rules?.first()

    if (regexMatcher == null || regexMatcher.rules.isEmpty() || regex !is RegexMatcher) {
      logger.debug { "No regex for '$expected', using equality" }
      return if (expected == actual) {
        emptyList()
      } else {
        listOf(BodyMismatch(expected, actual,
                "Expected body '$expected' to match '$actual' using equality but did not match"))
      }
    }

    return if (actual.matches(Regex(regex.regex))) {
      emptyList()
    } else {
      listOf(BodyMismatch(expected, actual,
              "Expected body '$expected' to match '$actual' using regex '${regex.regex}' but did not match"))
    }
  }

  companion object : KLogging()
}
