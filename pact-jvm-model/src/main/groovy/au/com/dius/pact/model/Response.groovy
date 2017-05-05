package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Response from a provider to a consumer
 */
@Canonical
class Response implements HttpPart {

  Integer status = 200
  Map<String, String> headers = [:]
  OptionalBody body = OptionalBody.missing()
  Map<String, Map<String, Object>> matchingRules = [:]

  static Response fromMap(def map) {
    new Response().with {
      status = map.status as Integer
      headers = map.headers ?: [:]
      body = map.containsKey('body') ? OptionalBody.body(map.body) : OptionalBody.missing()
      matchingRules = map.matchingRules ?: [:]
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
      matchingRules = r.matchingRules ? [:] + r.matchingRules : [:]
      it
    }
  }
}
