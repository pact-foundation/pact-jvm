package specification

import au.com.dius.pact.core.model.DefaultPactReader
import au.com.dius.pact.core.support.Json
import com.google.gson.JsonParser
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class BaseResponseSpec extends Specification {

  static List loadTestCases(String testDir) {
    def resources = ResponseSpecificationV1Spec.getResource(testDir)
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = f.withReader { new JsonParser().parse(it) }
        def jsonMap = Json.INSTANCE.toMap(json)
        def expected = DefaultPactReader.extractResponse(json.asJsonObject.get('expected').asJsonObject)
        def actual = DefaultPactReader.extractResponse(json.asJsonObject.get('actual').asJsonObject)
        if (expected.body.present) {
          expected.setDefaultContentType(expected.detectContentType())
        }
        actual.setDefaultContentType(actual.body.present ? actual.detectContentType() : 'application/json')
        result << [d.name, f.name, jsonMap.comment, jsonMap.match, jsonMap.match ? 'should match' : 'should not match',
                   expected, actual]
      }
    }
    result
  }

}
