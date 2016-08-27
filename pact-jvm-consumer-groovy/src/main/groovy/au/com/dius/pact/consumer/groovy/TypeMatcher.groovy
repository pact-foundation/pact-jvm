package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.NumberTypeMatcher
import org.apache.commons.lang3.RandomStringUtils

/**
 * Matcher for validating same types
 */
class TypeMatcher extends Matcher {

  def getMatcher() {
    switch (values.first()) {
      case 'integer':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
      case 'decimal':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
      case 'number':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
      default:
        return new au.com.dius.pact.model.matchingrules.TypeMatcher()
    }
  }

  def getValue() {
    if (values == null || values.empty || values.last() == null) {
      RandomStringUtils.randomNumeric(10)
    } else {
      values.last()
    }
  }

}
