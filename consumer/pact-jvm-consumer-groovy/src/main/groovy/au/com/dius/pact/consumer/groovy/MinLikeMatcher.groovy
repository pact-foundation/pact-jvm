package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.matchingrules.MatchingRule
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher

/**
 * Like matcher with a minimum size
 */
class MinLikeMatcher extends LikeMatcher {

  Integer min = 0

  MatchingRule getMatcher() {
    new MinTypeMatcher(min)
  }

}
