package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonObject
import spock.lang.Specification

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

}
