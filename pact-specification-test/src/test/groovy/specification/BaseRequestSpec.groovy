package specification

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import spock.lang.Specification

class BaseRequestSpec extends Specification {

  static List loadTestCases(String testDir) {
    def resources = BaseRequestSpec.getResource(testDir)
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = f.withReader { JsonParser.INSTANCE.parseReader(it) }
        def jsonMap = Json.INSTANCE.toMap(json)
        def expected = DefaultPactReader.extractRequest(json.asObject().get('expected').asObject())
        def actual = DefaultPactReader.extractRequest(json.asObject().get('actual').asObject())
        if (expected.body.present) {
          expected.setDefaultContentType(expected.body.detectContentType().toString())
        }
        actual.setDefaultContentType(actual.body.present ? actual.body.detectContentType().toString() :
          'application/json')
        result << [d.name, f.name, jsonMap.comment, jsonMap.match, jsonMap.match ? 'should match' : 'should not match',
                   expected, actual]
      }
    }
    result
  }

}
