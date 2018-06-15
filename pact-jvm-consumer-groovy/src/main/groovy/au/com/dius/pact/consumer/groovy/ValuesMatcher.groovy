package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.matchingrules.MatchingRule

/**
 * Matcher for validating the values in a map
 */
class ValuesMatcher extends Matcher {

  MatchingRule getMatcher() {
    au.com.dius.pact.core.model.matchingrules.ValuesMatcher.INSTANCE
  }

}
