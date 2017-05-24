package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher

/**
 * Like matcher with a maximum size
 */
class MaxLikeMatcher extends LikeMatcher {

  Integer max

  MatchingRule getMatcher() {
    new MaxTypeMatcher(max)
  }

}
