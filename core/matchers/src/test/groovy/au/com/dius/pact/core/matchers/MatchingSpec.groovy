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

  def 'Metadata Matching - match empty'() {
    expect:
    Matching.compareMessageMetadata([:], [:], null).empty
  }

  def 'Metadata Matching - match same metadata'() {
    expect:
    Matching.compareMessageMetadata([x: 1], [x: 1], null).empty
  }

  def 'Metadata Matching - ignore additional keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B'], [A: 'B', C: 'D'], null).empty
  }

  def 'Metadata Matching - complain about missing keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B', C: 'D'], [A: 'B'], null) == mismatch

    where:
    mismatch = [
      new MetadataMismatch('C', 'D', null, "Expected metadata 'C' but was missing")
    ]
  }

  def 'Metadata Matching - complain about incorrect keys'() {
    expect:
    Matching.compareMessageMetadata([A: 'B'], [A: 'C'], null) == mismatch

    where:
    mismatch = [
      new MetadataMismatch('A', 'B', 'C',
        "Expected metadata key 'A' to have value 'B' (String) but was 'C' (String)")
    ]
  }

  def 'Metadata Matching - ignores missing content type'() {
    expect:
    Matching.compareMessageMetadata([A: 'B', contentType: 'D'], [A: 'B'], null).empty
  }

}
