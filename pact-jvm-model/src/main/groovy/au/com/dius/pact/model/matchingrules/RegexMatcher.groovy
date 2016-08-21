package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Regular Expression Matcher
 */
@Canonical
class RegexMatcher implements MatchingRule {
  String regex
}
