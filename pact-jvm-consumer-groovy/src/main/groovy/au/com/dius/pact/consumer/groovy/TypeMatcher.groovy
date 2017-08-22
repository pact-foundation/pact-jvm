package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.model.matchingrules.MatchingRule
import au.com.dius.pact.model.matchingrules.NumberTypeMatcher

/**
 * Matcher for validating same types
 */
class TypeMatcher extends Matcher {

  String type = 'type'

  MatchingRule getMatcher() {
    switch (type) {
      case 'integer':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)
      case 'decimal':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL)
      case 'number':
        return new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER)
      default:
        return au.com.dius.pact.model.matchingrules.TypeMatcher.INSTANCE
    }
  }

}
