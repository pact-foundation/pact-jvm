package specification

import au.com.dius.pact.model.PactReader
import groovy.json.JsonSlurper
import spock.lang.Specification

class BaseResponseSpec extends Specification {

  static List loadTestCases(String testDir) {
    def resources = ResponseSpecificationV1Spec.getResource(testDir)
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
