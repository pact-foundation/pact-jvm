package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.PactReaderKt
import au.com.dius.pact.core.model.Request
import spock.lang.Specification

class MatchingSpec extends Specification {

  private static Request request

  def setup() {
    request = new Request('GET', '/', PactReaderKt.queryStringToMap('q=p&q=p2&r=s'),
      [testreqheader: 'testreqheadervalue'], OptionalBody.body('{"test": true}'.bytes))
  }

  def 'Header Matching - match empty'() {
    expect:
    Matching.matchHeaders(new Request('', '', null),
      new Request('', '', null)).empty
  }

  def 'Header Matching - match same headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: ['B']]), new Request('', '', null, [A: ['B']])).empty
  }

  def 'Header Matching - ignore additional headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: ['B']]), new Request('', '', null, [A: ['B'], C: ['D']])).empty
  }

  def 'Header Matching - complain about missing headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: ['B'], C: ['D']]),
      new Request('', '', null, [A: ['B']])) == mismatch

    where:
    mismatch = [
      new HeaderMismatch('C', 'D', '', "Expected a header 'C' but was missing")
    ]
  }

  def 'Header Matching - complain about incorrect headers'() {
    expect:
    Matching.matchHeaders(new Request('', '', null, [A: ['B']]), new Request('', '', null, [A: ['C']])) == mismatch

    where:
    mismatch = [
      new HeaderMismatch('A', 'B', 'C', "Expected header 'A' to have value 'B' but was 'C'")
    ]
  }

}
