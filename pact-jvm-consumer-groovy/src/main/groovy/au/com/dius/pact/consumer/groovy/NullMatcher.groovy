package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule

/**
 * Matcher to match null values
 */
class NullMatcher extends Matcher {
  MatchingRule getMatcher() {
    au.com.dius.pact.model.matchingrules.NullMatcher.INSTANCE
  }
}
