package au.com.dius.pact.consumer.groovy

class MinLikeMatcher extends LikeMatcher {

  def getMatcher() {
    [min: values.first(), match: 'type']
  }

  def getValue() {
    values.last()
  }

}
