package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Response from a provider to a consumer
 */
@Canonical
class Response implements HttpPart {

  Integer status = 200
  Map<String, String> headers
  String body
  Map<String, Map<String, Object>> matchingRules

  String toString() {
    "\tstatus: $status \n\theaders: $headers \n\tmatchers: $matchingRules \n\tbody: $body"
  }

  Response copy() {
    def r = this;
    new Response().with {
      status = r.status
      headers = r.headers
      body = r.body
      matchingRules = r.matchingRules
      it
    }
  }
}
