package specification

@java.lang.SuppressWarnings('UnusedImport')
import au.com.dius.pact.model.DiffConfig$
import au.com.dius.pact.model.ResponseMatching
import groovy.util.logging.Slf4j
import spock.lang.Unroll

@Slf4j
class ResponseSpecificationV1_1Spec extends BaseResponseSpec {

  @Unroll
  def '#type/#name - #test #matchDesc'() {
    expect:
    new ResponseMatching(DiffConfig$.MODULE$.apply(true, false)).responseMismatches(expected, actual).isEmpty() == match

    where:
    [type, name, test, match, matchDesc, expected, actual] << loadTestCases('/v1.1/response/')
  }

}
