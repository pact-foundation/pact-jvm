package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class TimeMatcher implements MatchingRule {
  String format
}
