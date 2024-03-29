package specification

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonParser
import au.com.dius.pact.provider.ResponseComparison
import groovy.json.JsonBuilder
import spock.lang.Specification
import spock.lang.Unroll

class MessageSpecificationSpec extends Specification {

  @Unroll
  @SuppressWarnings('UnnecessaryGetter')
  def '#test #matchDesc'() {
    expect:
    ResponseComparison.Companion.newInstance().compareMessage(expected, actual, null, [:])
      .bodyMismatches.value.mismatches.isEmpty() == match

    where:
    [test, match, matchDesc, expected, actual] << loadTestCases()
  }

  private static List loadTestCases() {
    def resources = MessageSpecificationSpec.getResource('/v3/message/')
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = f.withReader { JsonParser.INSTANCE.parseReader(it) }
        def jsonMap = Json.INSTANCE.toMap(json)
        result << [jsonMap.comment, jsonMap.match, jsonMap.match ? 'should match' : 'should not match',
                   Message.fromJson(json.asObject().get('expected').asObject()),
                   jsonMap.actual.contents ?
                     OptionalBody.body(new JsonBuilder(jsonMap.actual.contents).toPrettyString().bytes) :
                     OptionalBody.missing()]
      }
    }
    result
  }
}
