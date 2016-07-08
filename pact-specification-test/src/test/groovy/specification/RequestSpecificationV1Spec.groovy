package specification

import au.com.dius.pact.model.PactReader
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.RequestMatching
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
class RequestSpecificationV1Spec extends Specification {

  @Unroll
  def '#type #test #matchDesc'() {
    expect:
    RequestMatching.requestMismatches(expected, actual).isEmpty() == match

    where:
    [type, test, match, matchDesc, expected, actual] << loadTestCases()
  }

  private static List loadTestCases() {
    def resources = RequestSpecificationV1Spec.getResource('/v1/request/')
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = new JsonSlurper().parse(f)
        println json
        result << [d.name, json.comment, json.match, json.match ? 'should match' : 'should not match',
                   PactReader.extractRequestV2(json.expected),
                   PactReader.extractRequestV2(json.actual)]
      }
    }
    result
  }

}
