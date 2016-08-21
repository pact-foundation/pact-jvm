package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class TimestampMatcher implements MatchingRule {
  String format
}
