package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.ContentType.Companion.UNKNOWN
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import mu.KLogging

/**
 * Request made by a consumer to a provider
 */
interface IRequest: IHttpPart {
  var method: String
  var path: String
  val query: MutableMap<String, List<String>>
  override val headers: MutableMap<String, List<String>>
  override var body: OptionalBody
  override val matchingRules: MatchingRules
  override val generators: Generators

  fun cookies(): List<String>
  fun headersWithoutCookie(): Map<String, List<String>>
  fun asHttpPart() : HttpPart
  fun generatedRequest(context: MutableMap<String, Any>, mode: GeneratorTestMode): IRequest
  fun hasHeader(name: String): Boolean

  /**
   * If this request represents a multipart file upload
   */
  fun isMultipartFileUpload(): Boolean

  fun copy(): IRequest
}

/**
 * Request made by a consumer to a provider
 */
class Request @Suppress("LongParameterList") @JvmOverloads constructor(
  override var method: String = DEFAULT_METHOD,
  override var path: String = DEFAULT_PATH,
  override var query: MutableMap<String, List<String>> = mutableMapOf(),
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override var matchingRules: MatchingRules = MatchingRulesImpl(),
  override var generators: Generators = Generators()
) : BaseRequest(), Comparable<IRequest>, IRequest {

  override fun compareTo(other: IRequest) = if (equals(other)) 0 else 1

  override fun copy() = Request(method, path, query.toMutableMap(), headers.toMutableMap(), body.copy(),
    matchingRules.copy(), generators.copy())

  override fun generatedRequest(context: MutableMap<String, Any>, mode: GeneratorTestMode): IRequest {
    val r = this.copy()
    val pathGenerators = r.setupGenerators(Category.PATH, context)
    if (pathGenerators.isNotEmpty()) {
      Generators.applyGenerators(pathGenerators, mode) { _, g -> r.path = g.generate(context, r.path).toString() }
    }
    val headerGenerators = r.setupGenerators(Category.HEADER, context)
    if (headerGenerators.isNotEmpty()) {
      Generators.applyGenerators(headerGenerators, mode) { key, g ->
        r.headers[key] = listOf(g.generate(context, r.headers[key]).toString())
      }
    }
    val queryGenerators = r.setupGenerators(Category.QUERY, context)
    if (queryGenerators.isNotEmpty()) {
      Generators.applyGenerators(queryGenerators, mode) { key, g ->
        r.query[key] = r.query.getOrElse(key) { emptyList() }.map { g.generate(context, r.query[key]).toString() }
      }
    }
    if (r.body.isPresent()) {
      val bodyGenerators = r.setupGenerators(Category.BODY, context)
      if (bodyGenerators.isNotEmpty()) {
        r.body = Generators.applyBodyGenerators(bodyGenerators, r.body, determineContentType(), context, mode)
      }
    }
    return r
  }

  override fun hasHeader(name: String) = headers.any { (key, _) -> key.lowercase() == name }

  override fun toString(): String {
    return "\tmethod: $method\n\tpath: $path\n\tquery: $query\n\theaders: $headers\n\tmatchers: $matchingRules\n\t" +
      "generators: $generators\n\tbody: $body"
  }

  override fun headersWithoutCookie(): Map<String, List<String>> {
    return headers.filter { (k, _) -> k.toLowerCase() != COOKIE_KEY }
  }

  @Deprecated("use cookies()", ReplaceWith("cookies()"))
  fun cookie() = cookies()

  override fun cookies(): List<String> {
    val cookieEntry = headers.entries.find { (k, _) -> k.toLowerCase() == COOKIE_KEY }
    return if (cookieEntry != null) {
      cookieEntry.value.flatMap {
        it.split(';')
      }.map { it.trim() }
    } else {
      emptyList()
    }
  }

  override fun asHttpPart() = this

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other is IRequest) {
      if (method != other.method) return false
      if (path != other.path) return false
      if (query != other.query) return false
      if (headers != other.headers) return false
      if (body != other.body) return false
      if (matchingRules != other.matchingRules) return false
      if (generators != other.generators) return false

      return true
    }

    return false
  }

  override fun hashCode(): Int {
    var result = method.hashCode()
    result = 31 * result + path.hashCode()
    result = 31 * result + query.hashCode()
    result = 31 * result + headers.hashCode()
    result = 31 * result + body.hashCode()
    result = 31 * result + matchingRules.hashCode()
    result = 31 * result + generators.hashCode()
    return result
  }

  fun asV4Request(): HttpRequest {
    return HttpRequest(method, path, query, headers, body, matchingRules, generators)
  }

  companion object : KLogging() {
    const val COOKIE_KEY = "cookie"
    const val DEFAULT_METHOD = "GET"
    const val DEFAULT_PATH = "/"

    @JvmStatic
    fun fromJson(json: JsonValue.Object): Request {
      val method = if (json.has("method")) Json.toString(json["method"]) else DEFAULT_METHOD
      val path = if (json.has("path")) Json.toString(json["path"]) else DEFAULT_PATH
      val query = parseQueryParametersToMap(json["query"])
      val headers = if (json.has("headers") && json["headers"] is JsonValue.Object) {
        json["headers"].asObject()!!.entries.entries.associate { (key, value) ->
          key to HeaderParser.fromJson(key, value)
        }
      } else {
        emptyMap()
      }

      var contentType = UNKNOWN
      val contentTypeEntry = headers.entries.find { it.key.toUpperCase() == "CONTENT-TYPE" }
      if (contentTypeEntry != null) {
        contentType = ContentType(contentTypeEntry.value.first())
      }

      val body = if (json.has("body")) {
        extractBody(json, contentType)
      } else OptionalBody.missing()
      val matchingRules = if (json.has("matchingRules") && json["matchingRules"] is JsonValue.Object)
        MatchingRulesImpl.fromJson(json["matchingRules"])
        else MatchingRulesImpl()
      val generators = if (json.has("generators") && json["generators"] is JsonValue.Object)
        Generators.fromJson(json["generators"])
        else Generators()
      return Request(method, path, query.toMutableMap(), headers.toMutableMap(), body, matchingRules, generators)
    }
  }
}
