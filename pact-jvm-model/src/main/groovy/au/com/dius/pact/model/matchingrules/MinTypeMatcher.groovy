package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

@Canonical
class MinTypeMatcher implements MatchingRule {
  int min
}
