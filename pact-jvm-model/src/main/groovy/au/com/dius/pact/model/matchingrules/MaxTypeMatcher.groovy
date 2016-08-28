package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Type matching with a maximum size
 */
@Canonical
class MaxTypeMatcher implements MatchingRule {
  int max

  @Override
  Map toMap() {
    [match: 'type', max: max]
  }
}
