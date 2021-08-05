package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRuleCategory

interface ContentMatcher {
  fun matchBody(
    expected: OptionalBody,
    actual: OptionalBody,
    context: MatchingContext
  ): BodyMatchResult

  fun setupBodyFromConfig(bodyConfig: Map<String, Any?>): Triple<OptionalBody, MatchingRuleCategory?, Generators?>
}
