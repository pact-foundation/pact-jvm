package specification

import au.com.dius.pact.model.DiffConfig$
import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.ResponseMatching
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class ResponseSpecificationV1Spec extends Specification {

  @Unroll
  def '#type #test #matchDesc'() {
    expect:
    new ResponseMatching(DiffConfig$.MODULE$.apply(true, false)).responseMismatches(expected, actual).isEmpty() == match

    where:
    [type, test, match, matchDesc, expected, actual] << loadTestCases()
  }

  private static List loadTestCases() {
    def resources = ResponseSpecificationV1Spec.getResource('/v1/response/')
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = new JsonSlurper().parse(f)
        println json
        result << [d.name, json.comment, json.match, json.match ? 'should match' : 'should not match',
                   PactReader.extractResponse(json.expected),
                   PactReader.extractResponse(json.actual)]
      }
    }
    result
  }

}
