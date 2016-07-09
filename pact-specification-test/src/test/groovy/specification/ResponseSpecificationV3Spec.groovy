package specification

@java.lang.SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.DiffConfig$
import au.com.dius.pact.model.ResponseMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class ResponseSpecificationV3Spec extends BaseResponseSpec {

  @Unroll
  def '#type #test #matchDesc'() {
    expect:
    new ResponseMatching(DiffConfig$.MODULE$.apply(true, false)).responseMismatches(expected, actual).isEmpty() == match

    where:
    [type, test, match, matchDesc, expected, actual] << loadTestCases('/v3/response/')
  }

}
