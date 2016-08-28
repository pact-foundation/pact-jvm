package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Type matcher with a minimum size
 */
@Canonical
class MinTypeMatcher implements MatchingRule {
  int min

  @Override
  Map toMap() {
    [match: 'type', min: min]
  }
}
