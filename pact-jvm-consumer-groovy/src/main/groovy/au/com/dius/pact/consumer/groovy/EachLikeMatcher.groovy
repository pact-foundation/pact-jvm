package au.com.dius.pact.consumer.groovy

/**
 * Each like matcher for arrays
 */
class EachLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [match: 'type']
  }

  def getValue() {
    values.last()
  }

}
