package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule

/**
 * Each like matcher for arrays
 */
class EachLikeMatcher extends LikeMatcher {

  MatchingRule getMatcher() {
    new au.com.dius.pact.model.matchingrules.TypeMatcher()
  }

}
