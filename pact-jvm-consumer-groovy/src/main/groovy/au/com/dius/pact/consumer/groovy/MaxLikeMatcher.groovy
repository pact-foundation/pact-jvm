package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MaxTypeMatcher

/**
 * Like matcher with a maximum size
 */
class MaxLikeMatcher extends LikeMatcher {

  def getMatcher() {
    new MaxTypeMatcher(values.first())
  }

  def getValue() {
    values.last()
  }

}
