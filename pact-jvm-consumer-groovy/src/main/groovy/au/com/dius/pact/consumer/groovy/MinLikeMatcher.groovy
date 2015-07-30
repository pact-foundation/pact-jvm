package au.com.dius.pact.consumer.groovy

/**
 * Like matcher with a minimum size
 */
class MinLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [min: values.first(), match: 'type']
  }

  def getValue() {
    values.last()
  }

}
