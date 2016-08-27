package au.com.dius.pact.consumer.groovy

/**
 * Each like matcher for arrays
 */
class EachLikeMatcher extends LikeMatcher {

  def getMatcher() {
    new au.com.dius.pact.model.matchingrules.TypeMatcher()
  }

  def getValue() {
    values.last()
  }

}
