package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Matcher for types
 */
@Canonical
class TypeMatcher implements MatchingRule {

  @Override
  Map toMap() {
    [match: 'type']
  }
}
