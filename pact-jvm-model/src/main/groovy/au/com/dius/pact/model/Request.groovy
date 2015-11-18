package au.com.dius.pact.model

import groovy.transform.Canonical

/**
 * Request made by a consumer to a provider
 */
@Canonical
class Request implements HttpPart {
  String method
  String path
  Map<String, List<String>> query
  Map<String, String> headers
  String body
  Map<String, Map<String, Object>> matchingRules

  Request copy() {
    def r = this;
    new Request().with {
      method = r.method
      path = r.path
      query = r.query
      headers = r.headers
      body = r.body
      matchingRules = r.matchingRules
      it
    }
  }

  String toString() {
    "\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchingRules\n\tbody:\n$body"
  }

  Map<String, String> headersWithoutCookie() {
    headers.findAll { k, v -> k.toString().toLowerCase() != 'cookie' }
  }

  List<String> cookie() {
    def cookieEntry = headers.find { k, v ->
      k.toLowerCase() == 'cookie'
    }
    if (cookieEntry) {
      cookieEntry.value.split(';').collect{ it.trim() }
    } else {
      null
    }
  }

}
