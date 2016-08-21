package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Regular Expression Matcher
 */
@Canonical
class IncludeMatcher implements MatchingRule {
  String value
}
