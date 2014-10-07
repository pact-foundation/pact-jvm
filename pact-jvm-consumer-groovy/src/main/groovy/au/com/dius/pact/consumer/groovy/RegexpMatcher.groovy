package au.com.dius.pact.consumer.groovy

import nl.flotsam.xeger.Xeger

class RegexpMatcher extends Matcher {

  def getMatcher() {
    [regex: values[0].toString()]
  }

  def getValue() {
    if (values[1] == null) {
      new Xeger(values[0].toString()).generate()
    } else {
      values[1]
    }
  }

}
