package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonObject
import spock.lang.Specification
import spock.lang.Unroll

class ResponseSpec extends Specification {

  def 'delegates to the matching rules to parse matchers'() {
    given:
    def json = [
      matchingRules: [
        'stuff': ['': [matchers: [ [match: 'type'] ] ] ]
      ]
    ]

    when:
    def response = Response.fromJson(Json.INSTANCE.toJson(json).asJsonObject)

    then:
    !response.matchingRules.empty
    response.matchingRules.hasCategory('stuff')
  }

  def 'fromMap sets defaults for attributes missing from the map'() {
    expect:
    response.status == 200
    response.headers.isEmpty()
    response.body.isMissing()
    response.matchingRules.empty
    response.generators.empty

    where:
    response = Response.fromJson(new JsonObject())
  }

  @Unroll
  def 'fromMap should handle different number types'() {
    expect:
    Response.fromJson(Json.INSTANCE.toJson([status: statusValue]).asJsonObject).status == 200

    where:
    statusValue << [200, 200L, 200.0, 200.0G, 200G]
  }

}
