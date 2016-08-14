package specification

import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.RequestMatching
import spock.lang.Unroll

class RequestSpecificationV3Spec extends BaseRequestSpec {

  public static final WIP_LIST = [
    'body/array size less than required xml.json',
    'body/array size less than required.json',
    'body/array with at least one element matching by example.json',
    'body/array with at least one element matching by example xml.json',
    'body/array with nested array that matches.json',
    'body/array with regular expression in element.json',
    'body/array with regular expression in element xml.json',
    'body/matches with regex with bracket notation.json',
    'body/matches with regex with bracket notation xml.json',
    'body/matches with regex xml.json',
    'body/matches with regex.json',
    'body/matches with type.json',
    'headers/matches with regex.json',
    'path/matches with regex.json',
    'query/matches with regex.json'
  ]

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).isEmpty() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << wipFilter(
      loadTestCases('/v3/request/', PactSpecVersion.V3))
  }

  static List wipFilter(List tests) {
    tests.collect {
      def test = "${it[0]}/${it[1]}"
      if (WIP_LIST.any { it == test }) {
        it[3] = !it[3]
        it[0] = 'WIP ' + it[0]
        it
      } else {
        it
      }
    }
  }

}
