package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory

class PluginContentMatcher(
  val contentMatcher: io.pact.plugins.jvm.core.ContentMatcher,
  val contentType: ContentType
) : ContentMatcher {
  override fun matchBody(expected: OptionalBody, actual: OptionalBody, context: MatchingContext): BodyMatchResult {
    val result = contentMatcher.invokeContentMatcher(expected, actual, context.allowUnexpectedKeys,
      context.matchers.matchingRules)
    val bodyResults = emptyList<BodyItemMatchResult>()
    return BodyMatchResult(null, bodyResults)
  }

  override fun setupBodyFromConfig(bodyConfig: Map<String, Any?>): Triple<OptionalBody, MatchingRuleCategory?, Generators?> {
    return contentMatcher.configureContent(contentType.toString(), bodyConfig)
  }
}
