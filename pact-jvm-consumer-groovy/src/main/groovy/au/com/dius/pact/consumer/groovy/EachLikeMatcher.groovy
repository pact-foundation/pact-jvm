package au.com.dius.pact.consumer.groovy

class EachLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [min: 0]
  }

  def getValue() {
    values.last()
  }

}
