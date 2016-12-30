package au.com.dius.pact.model

import au.com.dius.pact.model.generators.ExampleGenerators
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.matchingrules.MatchingRules
import groovy.transform.Canonical

/**
 * Response from a provider to a consumer
 */
@Canonical
class Response implements HttpPart {

  Integer status = 200
  Map<String, String> headers = [:]
  OptionalBody body = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRules()
  Generators generators = new ExampleGenerators()

  static Response fromMap(def map) {
    new Response().with {
      status = map.status as Integer
      headers = map.headers
      body = map.containsKey('body') ? OptionalBody.body(map.body) : OptionalBody.missing()
      matchingRules = MatchingRules.fromMap(map.matchingRules)
      it
    }
  }

  String toString() {
    "\tstatus: $status \n\theaders: $headers \n\tmatchers: $matchingRules \n\tbody: $body"
  }

  Response copy() {
    def r = this
    new Response().with {
      status = r.status
      headers = r.headers ? [:] + r.headers : null
      body = r.body
      matchingRules = r.matchingRules.copy()
      it
    }
  }
}
