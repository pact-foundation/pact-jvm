package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Type matching for numbers
 */
@Canonical
class NumberTypeMatcher implements MatchingRule {
  enum NumberType {
    NUMBER,
    INTEGER,
    DECIMAL
  }

  NumberType numberType

  @Override
  Map toMap() {
    [match: numberType.name().toLowerCase()]
  }
}
