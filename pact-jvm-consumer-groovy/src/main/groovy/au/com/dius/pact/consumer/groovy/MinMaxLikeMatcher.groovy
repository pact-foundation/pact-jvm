package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.MinMaxTypeMatcher

/**
 * Like Matcher with a minimum and maximum size
 */
class MinMaxLikeMatcher extends LikeMatcher {
  Integer min, max

  MatchingRule getMatcher() {
    new MinMaxTypeMatcher(min, max)
  }
}
