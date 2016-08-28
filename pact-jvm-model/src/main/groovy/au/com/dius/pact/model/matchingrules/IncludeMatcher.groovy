package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Regular Expression Matcher
 */
@Canonical
class IncludeMatcher implements MatchingRule {
  String value

  @Override
  Map toMap() {
    [match: 'include', value: value]
  }
}
