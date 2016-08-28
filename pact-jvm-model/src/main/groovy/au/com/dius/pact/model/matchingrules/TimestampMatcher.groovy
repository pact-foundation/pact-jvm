package au.com.dius.pact.model.matchingrules

import groovy.transform.Canonical

/**
 * Matching rule for timestamp values
 */
@Canonical
class TimestampMatcher implements MatchingRule {
  String format

  @Override
  Map toMap() {
    [match: 'timestamp', timestamp: format]
  }
}
