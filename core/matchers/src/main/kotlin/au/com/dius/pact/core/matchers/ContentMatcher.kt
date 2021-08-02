package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody

interface ContentMatcher {
  fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult
}
