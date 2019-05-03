package au.com.dius.pact.matchers

import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import spock.lang.Specification

class QueryMatcherSpec extends Specification {

  def 'uses equality by default'() {
    expect:
    QueryMatcher.compareQuery('a', ['1', '2'], ['1', '3'], new MatchingRulesImpl())*.mismatch ==
      ["Expected '2' but received '3' for query parameter 'a'"]
  }

  def 'checks the number of parameters'() {
    expect:
    QueryMatcher.compareQuery('a', ['1', '2'], ['1'], new MatchingRulesImpl())*.mismatch ==
      ["Expected query parameter 'a' with 2 values but received 1 value",
       "Expected query parameter 'a' with value '2' but was missing"]
  }

  def 'applies matching rules to the parameter values'() {
    expect:
    QueryMatcher.compareQuery('a', ['1000-01-01', '2000-01-01'], ['2000-01-01', '2000x-01-03'],
      MatchingRulesImpl.fromMap([
        query: [
          a: [ matchers: [ [match: 'date', format: 'yyyy-MM-dd'] ] ]
        ]
      ])
    )*.mismatch == ["Expected '2000x-01-03' to match a date of 'yyyy-MM-dd': Unable to parse the date: 2000x-01-03"]
  }

}
