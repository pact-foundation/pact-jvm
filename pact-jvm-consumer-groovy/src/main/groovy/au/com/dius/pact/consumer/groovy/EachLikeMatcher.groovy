package au.com.dius.pact.consumer.groovy

class EachLikeMatcher extends LikeMatcher {

  def getMatcher() {
    null
  }

  def getValue() {
    values.last()
  }

}
