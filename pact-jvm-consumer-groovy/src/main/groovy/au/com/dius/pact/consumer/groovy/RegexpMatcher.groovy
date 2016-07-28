package au.com.dius.pact.consumer.groovy

import com.mifmif.common.regex.Generex

/**
 * Regular Expression Matcher
 */
class RegexpMatcher extends Matcher {

  def getMatcher() {
    [regex: values[0].toString()]
  }

  def getValue() {
    if (values[1] == null) {
      new Generex(values[0].toString()).random()
    } else {
      values[1]
    }
  }

}
