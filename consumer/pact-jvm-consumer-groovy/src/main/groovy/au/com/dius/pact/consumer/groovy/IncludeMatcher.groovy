package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.matchingrules.MatchingRule

/**
 * Matcher for string inclusion
 */
class IncludeMatcher extends Matcher {
  MatchingRule getMatcher() {
    new au.com.dius.pact.core.model.matchingrules.IncludeMatcher(value)
  }
}
