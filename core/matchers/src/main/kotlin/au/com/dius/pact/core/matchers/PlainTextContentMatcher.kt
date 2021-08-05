package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import mu.KLogging

class PlainTextContentMatcher : ContentMatcher {

  override fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult {
    return when {
      expected.isMissing() -> BodyMatchResult(null, emptyList())
      expected.isNull() && actual.isPresent() -> BodyMatchResult(null, listOf(
        BodyItemMatchResult("$",
          listOf(BodyMismatch(null, actual!!.value, "Expected empty body but received '${actual.value}'")))))
      expected.isNull() -> BodyMatchResult(null, emptyList())
      actual.isMissing() -> BodyMatchResult(null, listOf(BodyItemMatchResult("$",
        listOf(BodyMismatch(expected!!.value, null,
          "Expected body '${expected.value}' but was missing")))))
      expected.isEmpty() && actual.isEmpty() -> BodyMatchResult(null, emptyList())
      else -> BodyMatchResult(null,
        compareText(expected.valueAsString(), actual.valueAsString(), context))
    }
  }

  fun compareText(expected: String, actual: String, context: MatchingContext): List<BodyItemMatchResult> {
    val regexMatcher = context.matchers.matchingRules["$"]
    val regex = regexMatcher?.rules?.first()

    if (regexMatcher == null || regexMatcher.rules.isEmpty() || regex !is RegexMatcher) {
      logger.debug { "No regex for '$expected', using equality" }
      return if (expected == actual) {
        listOf(BodyItemMatchResult("$", emptyList()))
      } else {
        listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expected, actual,
          "Expected body '$expected' to match '$actual' using equality but did not match"))))
      }
    }

    return if (actual.matches(Regex(regex.regex))) {
      emptyList()
    } else {
      listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expected, actual,
        "Expected body '$expected' to match '$actual' using regex '${regex.regex}' but did not match"))))
    }
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Triple<OptionalBody, MatchingRuleCategory?, Generators?> {
    return Triple(
      OptionalBody.body(
        bodyConfig["body"].toString().toByteArray(),
        ContentType("text/plain")
      ),
      null,
      null
    )
  }

  companion object : KLogging()
}
