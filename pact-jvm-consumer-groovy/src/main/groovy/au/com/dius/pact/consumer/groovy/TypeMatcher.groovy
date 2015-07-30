package au.com.dius.pact.consumer.groovy

import org.apache.commons.lang3.RandomStringUtils

/**
 * Matcher for validating same types
 */
class TypeMatcher extends Matcher {

  def getMatcher() {
    [match: values.first()]
  }

  def getValue() {
    if (values == null || values.empty || values.last() == null) {
      RandomStringUtils.randomNumeric(10)
    } else {
      values.last()
    }
  }

}
