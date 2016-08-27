package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MinTypeMatcher

/**
 * Like matcher with a minimum size
 */
class MinLikeMatcher extends LikeMatcher {

  def getMatcher() {
    new MinTypeMatcher(values.first())
  }

  def getValue() {
    values.last()
  }

}
