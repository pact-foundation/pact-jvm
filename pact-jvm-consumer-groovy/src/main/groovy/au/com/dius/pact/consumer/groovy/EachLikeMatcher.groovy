package au.com.dius.pact.consumer.groovy

class EachLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [match: 'type']
  }

  def getValue() {
    values.last()
  }

}
