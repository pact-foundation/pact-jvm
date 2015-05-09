package au.com.dius.pact.consumer.groovy

class MaxLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [max: values.first()]
  }

  def getValue() {
    values.last()
  }

}
