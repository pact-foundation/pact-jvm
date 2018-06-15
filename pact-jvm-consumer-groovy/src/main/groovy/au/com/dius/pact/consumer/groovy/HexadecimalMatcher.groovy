package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.RegexMatcher

/**
 * Matcher for hexadecimal values
 */
class HexadecimalMatcher extends Matcher {

  MatchingRule getMatcher() {
    new RegexMatcher(Matchers.HEXADECIMAL)
  }

  Generator getGenerator() {
    super.@value == null ? new RandomHexadecimalGenerator(10) : null
  }

  def getValue() {
    super.@value ?: '1234a'
  }

}
