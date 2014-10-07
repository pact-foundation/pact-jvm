package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils

class TypeMatcher extends Matcher {

  def getMatcher() {
    [match: 'type']
  }

  def getValue() {
    if (values == null) {
      RandomStringUtils.randomNumeric(10)
    } else {
      values
    }
  }

}
