package au.com.dius.pact.model

import au.com.dius.pact.model.generators.ExampleGenerators
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.matchingrules.MatchingRules
import groovy.transform.Canonical

/**
 * Request made by a consumer to a provider
 */
@Canonical
class Request implements HttpPart {
  private static final String COOKIE_KEY = 'cookie'

  String method = 'GET'
  String path = '/'
  Map<String, List<String>> query = [:]
  Map<String, String> headers = [:]
  OptionalBody body = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRules()
  Generators generators = new ExampleGenerators()

  static Request fromMap(Map map) {
    new Request().with {
      method = map.method as String
      path = map.path as String
      query = map.query
      headers = map.headers
      body = map.containsKey('body') ? OptionalBody.body(map.body) : OptionalBody.missing()
      matchingRules = MatchingRules.fromMap(map.matchingRules)
      it
    }
  }

  Request copy() {
    def r = this
    new Request().with {
      method = r.method
      path = r.path
      query = r.query ? [:] + r.query : null
      headers = r.headers ? [:] + r.headers : null
      body = r.body
      matchingRules = r.matchingRules.copy()
      it
    }
  }

  String toString() {
    "\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchingRules\n\tbody: $body"
  }

  Map<String, String> headersWithoutCookie() {
    headers?.findAll { k, v -> k.toLowerCase() != COOKIE_KEY }
  }

  List<String> cookie() {
    def cookieEntry = headers?.find { k, v ->
      k.toLowerCase() == COOKIE_KEY
    }
    if (cookieEntry) {
      cookieEntry.value.split(';')*.trim()
    } else {
      null
    }
  }
}
