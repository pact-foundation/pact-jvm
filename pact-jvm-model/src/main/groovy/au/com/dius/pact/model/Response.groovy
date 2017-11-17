package au.com.dius.pact.model

import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.generators.Category
import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import groovy.transform.Canonical

/**
 * Response from a provider to a consumer
 */
@Canonical
class Response extends HttpPart {

  public static final int DEFAULT_STATUS = 200

  Integer status = DEFAULT_STATUS
  Map<String, String> headers = [:]
  OptionalBody body = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRulesImpl()
  Generators generators = new Generators()

  static Response fromMap(def map) {
    new Response().with {
      status = (map.status ?: DEFAULT_STATUS) as Integer
      headers = map.headers ?: [:]
      body = map.containsKey('body') ? OptionalBody.body(map.body) : OptionalBody.missing()
      matchingRules = MatchingRulesImpl.fromMap(map.matchingRules)
      generators = Generators.fromMap(map.generators)
      it
    }
  }

  String toString() {
    "\tstatus: $status\n\theaders: $headers\n\tmatchers: $matchingRules\n\tgenerators: $generators\n\tbody: $body"
  }

  Response copy() {
    def r = this
    new Response().with {
      status = r.status
      headers = r.headers ? [:] + r.headers : [:]
      body = r.body
      matchingRules = r.matchingRules.copy()
      generators = r.generators.copy(r.generators.categories)
      it
    }
  }

  Response generatedResponse() {
    def r = this.copy()
    generators.applyGenerator(Category.STATUS) { String key, Generator g -> r.status = g.generate(r.status) as Integer }
    generators.applyGenerator(Category.HEADER) { String key, Generator g ->
      r.headers[key] = g.generate(r.headers[key])
    }
    r.body = generators.applyBodyGenerators(r.body, new ContentType(mimeType()))
    r
  }
}
