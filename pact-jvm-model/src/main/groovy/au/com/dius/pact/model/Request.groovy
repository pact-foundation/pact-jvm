package au.com.dius.pact.model

import au.com.dius.pact.model.generators.Category
import au.com.dius.pact.model.generators.Generator
import au.com.dius.pact.model.generators.Generators
import au.com.dius.pact.model.matchingrules.MatchingRules
import au.com.dius.pact.model.matchingrules.MatchingRulesImpl
import groovy.transform.Canonical
import org.jetbrains.annotations.NotNull

/**
 * Request made by a consumer to a provider
 */
@Canonical
class Request extends BaseRequest implements Comparable {
  private static final String COOKIE_KEY = 'cookie'

  public static final String DEFAULT_METHOD = 'GET'
  public static final String DEFAULT_PATH = '/'

  String method = DEFAULT_METHOD
  String path = DEFAULT_PATH
  Map<String, List<String>> query = [:]
  Map<String, String> headers = [:]
  OptionalBody body = OptionalBody.missing()
  MatchingRules matchingRules = new MatchingRulesImpl()
  Generators generators = new Generators()

  static Request fromMap(Map map) {
    new Request().with {
      method = (map.method ?: DEFAULT_METHOD) as String
      path = (map.path == null ? DEFAULT_PATH : map.path) as String
      query = map.query ?: [:]
      headers = map.headers ?: [:]
      body = map.containsKey('body') ? OptionalBody.body(map.body) : OptionalBody.missing()
      matchingRules = MatchingRulesImpl.fromMap(map.matchingRules)
      generators = Generators.fromMap(map.generators)
      it
    }
  }

  Request copy() {
    def r = this
    new Request().with {
      method = r.method
      path = r.path
      query = r.query ? [:] + r.query : [:]
      headers = r.headers ? [:] + r.headers : [:]
      body = r.body
      matchingRules = r.matchingRules.copy()
      generators = r.generators.copy(r.generators.categories)
      it
    }
  }

  Request generatedRequest(Map context = [:]) {
    def r = this.copy()
    generators.applyGenerator(Category.PATH) { String key, Generator g -> r.path = g.generate(context).toString() }
    generators.applyGenerator(Category.HEADER) { String key, Generator g ->
      r.headers[key] = g.generate(context).toString()
    }
    generators.applyGenerator(Category.QUERY) { String key, Generator g ->
      r.query[key] = r.query[key].collect { g.generate(context).toString() }
    }
    r.body = generators.applyBodyGenerators(r.body, new ContentType(mimeType()), context)
    r
  }

  String toString() {
    "\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchingRules\n\t" +
      "generators: $generators\n\tbody: $body"
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

  @Override
  @SuppressWarnings('ExplicitCallToEqualsMethod')
  int compareTo(@NotNull Object o) {
    equals(o) ? 0 : 1
  }
}
