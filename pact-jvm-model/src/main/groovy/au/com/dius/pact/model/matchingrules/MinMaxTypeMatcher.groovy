package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Type matcher with a minimum size and maximum size
 */
@Canonical
class MinMaxTypeMatcher implements MatchingRule {
  int min, max

  @Override
  Map toMap() {
    [match: 'type', min: min, max: max]
  }
}
