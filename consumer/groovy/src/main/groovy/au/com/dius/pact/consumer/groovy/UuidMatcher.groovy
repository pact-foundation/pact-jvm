package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.UuidGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.RegexMatcher

/**
 * Matcher for universally unique IDs
 */
class UuidMatcher extends Matcher {

  MatchingRule getMatcher() {
    new RegexMatcher(Matchers.UUID_REGEX)
  }

  Generator getGenerator() {
    super.@value == null ? UuidGenerator.INSTANCE : null
  }

  def getValue() {
    super.@value ?: 'e2490de5-5bd3-43d5-b7c4-526e33f71304'
  }

}
