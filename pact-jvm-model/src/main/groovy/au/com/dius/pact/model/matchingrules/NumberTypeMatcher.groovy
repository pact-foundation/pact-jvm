package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class NumberTypeMatcher implements MatchingRule {
  enum NumberType {
    NUMBER,
    INTEGER,
    DECIMAL
  }

  NumberType numberType
}
