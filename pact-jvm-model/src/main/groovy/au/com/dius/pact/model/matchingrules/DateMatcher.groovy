package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class DateMatcher implements MatchingRule {
  String format
}
