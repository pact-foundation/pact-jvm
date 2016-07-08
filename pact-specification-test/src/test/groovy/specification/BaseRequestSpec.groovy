package specification

import au.com.dius.pact.model.PactReader
import groovy.json.JsonSlurper
import spock.lang.Specification

class BaseRequestSpec extends Specification {

  static List loadTestCases(String testDir) {
    def resources = RequestSpecificationV1_1Spec.getResource(testDir)
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
