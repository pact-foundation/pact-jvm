package au.com.dius.pact.consumer.groovy

class MaxLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [max: values.first(), match: 'type']
  }

  def getValue() {
    values.last()
  }

}
