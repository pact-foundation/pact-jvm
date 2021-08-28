package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import io.pact.plugins.jvm.core.InteractionContents

interface ContentMatcher {
  fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult

  fun setupBodyFromConfig(bodyConfig: Map<String, Any?>): InteractionContents
}
