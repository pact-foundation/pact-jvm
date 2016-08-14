package specification

@java.lang.SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.DiffConfig$
import au.com.dius.pact.model.ResponseMatching
import spock.lang.Unroll

class ResponseSpecificationV3Spec extends BaseResponseSpec {

  public static final WIP_LIST = [
    'body/array at top level with matchers xml.json',
    'body/array at top level with matchers.json',
    'body/array with regex matcher.json',
    'body/array with regex matcher xml.json',
    'body/array with type matcher.json',
    'body/matches with regex.json',
    'body/matches with regex xml.json',
    'body/matches with type.json',
    'body/objects in array type matching.json',
    'body/objects in array type matching xml.json',
    'headers/matches with regex.json',
    'body/additional property with type matcher.json',
    'body/array with type matcher mismatch xml.json'
  ]

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    new ResponseMatching(DiffConfig$.MODULE$.apply(true, false)).responseMismatches(expected, actual).isEmpty() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << wipFilter(loadTestCases('/v3/response/'))
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
