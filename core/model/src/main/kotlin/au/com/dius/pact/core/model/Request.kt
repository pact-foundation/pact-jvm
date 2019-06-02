package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import mu.KLogging

/**
 * Request made by a consumer to a provider
 */
class Request @JvmOverloads constructor(
  var method: String = DEFAULT_METHOD,
  var path: String = DEFAULT_PATH,
  var query: MutableMap<String, List<String>> = mutableMapOf(),
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override var matchingRules: MatchingRules = MatchingRulesImpl(),
  var generators: Generators = Generators()
): BaseRequest(), Comparable<Request> {

  override fun compareTo(other: Request) = if (equals(other)) 0 else 1

  fun copy() = Request(method, path, query.toMutableMap(), headers.toMutableMap(), body.copy(), matchingRules.copy(),
    generators.copy())

  fun generatedRequest(context: Map<String, Any> = emptyMap(), mode: GeneratorTestMode = GeneratorTestMode.Provider): Request {
    val r = this.copy()
    generators.applyGenerator(Category.PATH, mode) { _, g -> r.path = g.generate(context).toString() }
    generators.applyGenerator(Category.HEADER, mode) { key, g ->
      r.headers[key] = listOf(g.generate(context).toString())
    }
    generators.applyGenerator(Category.QUERY, mode) { key, g ->
      r.query[key] = r.query.getOrElse(key) { emptyList() }.map { g.generate(context).toString() }
    }
    r.body = generators.applyBodyGenerators(r.body, ContentType(mimeType()), context, mode)
    return r
  }

  override fun toString(): String {
    return "\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchingRules\n\t" +
      "generators: $generators\n\tbody: $body"
  }

  fun headersWithoutCookie(): Map<String, List<String>> {
    return headers.filter { (k, _) -> k.toLowerCase() != COOKIE_KEY }
  }

  fun cookie(): List<String> {
    val cookieEntry = headers.entries.find { (k, v) -> k.toLowerCase() == COOKIE_KEY }
    return if (cookieEntry != null) {
      cookieEntry.value.flatMap {
        it.split(';')
      }.map { it.trim() }
    } else {
      emptyList()
    }
  }

  companion object: KLogging() {
    const val COOKIE_KEY = "cookie"
    const val DEFAULT_METHOD = "GET"
    const val DEFAULT_PATH = "/"

    @JvmStatic
    fun fromMap(map: Map<String, Any>): Request {
      val method = (map["method"] ?: DEFAULT_METHOD).toString()
      val path = (map["path"] ?: DEFAULT_PATH).toString()
      val query = parseQueryParametersToMap(map["query"])
      val headers = (map.getOrDefault("headers", emptyMap<String, Any>()) as Map<String, Any>).entries
        .associate { (key, value) ->
          if (value is List<*>) {
            key to value as List<String>
          } else {
            key to value.toString().split(",").map { it.trim() }
          }
        }
      val body = if (map.containsKey("body"))
        OptionalBody.body(map["body"]?.toString()?.toByteArray())
        else OptionalBody.missing()
      val matchingRules = MatchingRulesImpl.fromMap(map["matchingRules"] as Map<String, Map<String, Any?>>)
      val generators = Generators.fromMap(map["generators"] as Map<String, Map<String, Any>>)
      return Request(method, path, query.toMutableMap(), headers.toMutableMap(), body, matchingRules, generators)
    }
  }
}
