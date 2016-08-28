package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Matching Rule for dates
 */
@Canonical
class DateMatcher implements MatchingRule {
  String format
}
