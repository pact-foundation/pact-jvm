package specification

import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.model.PactSpecVersion
import groovy.json.JsonSlurper
import spock.lang.Specification

class BaseRequestSpec extends Specification {

  static List loadTestCases(String testDir, PactSpecVersion version) {
    def resources = BaseRequestSpec.getResource(testDir)
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = new JsonSlurper().parse(f)
        def actual, expected
        if (version == PactSpecVersion.V3) {
          expected = PactReader.extractRequestV3(json.expected)
          actual = PactReader.extractRequestV3(json.actual)
        } else {
          expected = PactReader.extractRequestV2(json.expected)
          actual = PactReader.extractRequestV2(json.actual)
        }
        if (expected.body.present) {
          expected.setDefaultMimeType(expected.detectContentType())
        }
        actual.setDefaultMimeType(actual.body.present ? actual.detectContentType() : 'application/json')
        result << [d.name, f.name, json.comment, json.match, json.match ? 'should match' : 'should not match',
                   expected, actual]
      }
    }
    result
  }

}
