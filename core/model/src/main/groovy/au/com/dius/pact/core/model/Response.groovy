package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import groovy.transform.Canonical

/**
 * Response from a provider to a consumer
 */
@Canonical
class Response extends BaseResponse {

  public static final int DEFAULT_STATUS = 200

  Integer status = DEFAULT_STATUS
  Map<String, List<String>> headers = [:]
  OptionalBody body = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRulesImpl()
  Generators generators = new Generators()

  static Response fromMap(def map) {
    new Response().with {
      status = (map.status ?: DEFAULT_STATUS) as Integer
      headers = map.headers ? map.headers.collectEntries { key, value ->
        if (value instanceof List) {
          [key, value]
        } else {
          [key, value.split(/,/)*.trim() ]
        }
      } : [:]
      body = map.containsKey('body') ? OptionalBody.body(map.body?.bytes) : OptionalBody.missing()
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

  Response generatedResponse(Map context = [:], GeneratorTestMode mode = GeneratorTestMode.Provider) {
    def r = this.copy()
    generators.applyGenerator(Category.STATUS, mode) { String key, Generator g ->
      r.status = g.generate(context) as Integer
    }
    generators.applyGenerator(Category.HEADER, mode) { String key, Generator g ->
      r.headers[key] = [ g.generate(context).toString() ]
    }
    r.body = generators.applyBodyGenerators(r.body, new ContentType(mimeType()), context, mode)
    r
  }
}
