package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import io.pact.plugins.jvm.core.InteractionContents

class PluginContentMatcher(
  val contentMatcher: io.pact.plugins.jvm.core.ContentMatcher,
  val contentType: ContentType
) : ContentMatcher {
  override fun matchBody(expected: OptionalBody, actual: OptionalBody, context: MatchingContext): BodyMatchResult {
    val result = contentMatcher.invokeContentMatcher(expected, actual, context.allowUnexpectedKeys,
      context.matchers.matchingRules)
    val bodyResults = result.entries.map { mismatch ->
      BodyItemMatchResult(mismatch.key, mismatch.value.map {
        BodyMismatch(it.expected, it.actual, it.mismatch, it.path, it.diff)
      })
    }
    return BodyMatchResult(null, bodyResults)
  }

  override fun setupBodyFromConfig(
    bodyConfig: Map<String, Any?>
  ): InteractionContents {
    return contentMatcher.configureContent(contentType.toString(), bodyConfig)
  }
}
