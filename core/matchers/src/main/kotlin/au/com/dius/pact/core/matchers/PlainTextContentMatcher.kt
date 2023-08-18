package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.support.Result
import io.pact.plugins.jvm.core.InteractionContents
import io.github.oshai.kotlinlogging.KLogging

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
    val matchers = context.matchers.matchingRules["$"]
    val regexMatcher = matchers?.rules?.first()

    if (matchers == null || matchers.rules.isEmpty() || regexMatcher !is RegexMatcher) {
      logger.debug { "No regex for '$expected', using equality" }
      return if (expected == actual) {
        listOf(BodyItemMatchResult("$", emptyList()))
      } else {
        listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expected, actual,
          "Expected body '$expected' to match '$actual' using equality but did not match"))))
      }
    }

    val regex = Regex(regexMatcher.regex, setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
    return if (regex.matches(actual)) {
      emptyList()
    } else {
      listOf(BodyItemMatchResult("$", listOf(BodyMismatch(expected, actual,
        "Expected body '$expected' to match '$actual' using regex '${regexMatcher.regex}' but did not match"))))
    }
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): Result<List<InteractionContents>, String> {
    return Result.Ok(listOf(InteractionContents("",
      OptionalBody.body(
        bodyConfig["body"].toString().toByteArray(),
        ContentType("text/plain")
      )
    )))
  }

  companion object : KLogging()
}
