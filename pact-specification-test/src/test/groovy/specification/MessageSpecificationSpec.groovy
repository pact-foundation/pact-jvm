package specification

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.provider.ResponseComparison
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

class MessageSpecificationSpec extends Specification {

  @Unroll
  def '#test #matchDesc'() {
    expect:
    ResponseComparison.compareMessage(expected, actual).body.isEmpty() == match

    where:
    [test, match, matchDesc, expected, actual] << loadTestCases()
  }

  private static List loadTestCases() {
    def resources = MessageSpecificationSpec.getResource('/v3/message/')
    def file = new File(resources.toURI())
    def result = []
    file.eachDir { d ->
      d.eachFile { f ->
        def json = new JsonSlurper().parse(f)
        result << [json.comment, json.match, json.match ? 'should match' : 'should not match',
                   Message.fromMap(json.expected),
                   json.actual.contents ?
                     OptionalBody.body(new JsonBuilder(json.actual.contents).toPrettyString().bytes) :
                     OptionalBody.missing()]
      }
    }
    result
  }

}
