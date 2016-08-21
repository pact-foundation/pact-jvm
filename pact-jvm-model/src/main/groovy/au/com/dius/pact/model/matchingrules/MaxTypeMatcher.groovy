package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class MaxTypeMatcher implements MatchingRule {
  int max
}
