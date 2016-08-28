package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Regular Expression Matcher
 */
@Canonical
class RegexMatcher implements MatchingRule {
  String regex

  @Override
  Map toMap() {
    [match: 'regex', regex: regex]
  }
}
