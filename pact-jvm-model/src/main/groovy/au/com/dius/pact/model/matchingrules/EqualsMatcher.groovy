package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Matching rule for equality
 */
@Canonical
class EqualsMatcher implements MatchingRule {
  @Override
  Map toMap() {
    [match: 'equality']
  }
}
