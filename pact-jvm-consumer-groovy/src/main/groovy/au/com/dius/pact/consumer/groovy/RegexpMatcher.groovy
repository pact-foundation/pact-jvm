package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.RegexMatcher
import com.mifmif.common.regex.Generex

/**
 * Regular Expression Matcher
 */
class RegexpMatcher extends Matcher {

  def getMatcher() {
    new RegexMatcher(values[0].toString())
  }

  def getValue() {
    if (values[1] == null) {
      new Generex(values[0].toString()).random()
    } else {
      values[1]
    }
  }

}
