package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Matcher for time values
 */
@Canonical
class TimeMatcher implements MatchingRule {
  String format

  @Override
  Map toMap() {
    [match: 'time', time: format]
  }
}
