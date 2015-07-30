package au.com.dius.pact.consumer.groovy

/**
 * Like matcher with a maximum size
 */
class MaxLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [max: values.first(), match: 'type']
  }

  def getValue() {
    values.last()
  }

}
