package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.core.model.matchingrules.MatchingRule

/**
 * Each like matcher for arrays
 */
class EachLikeMatcher extends LikeMatcher {

  MatchingRule getMatcher() {
    au.com.dius.pact.core.model.matchingrules.TypeMatcher.INSTANCE
  }

}
