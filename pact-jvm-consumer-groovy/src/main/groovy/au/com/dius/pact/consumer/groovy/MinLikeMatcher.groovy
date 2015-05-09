package au.com.dius.pact.consumer.groovy

class MinLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [min: values.first()]
  }

  def getValue() {
    values.last()
  }

}
