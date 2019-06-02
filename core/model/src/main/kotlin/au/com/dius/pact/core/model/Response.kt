package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import mu.KLogging

/**
 * Response from a provider to a consumer
 */
class Response @JvmOverloads constructor(
  var status: Int = DEFAULT_STATUS,
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override var matchingRules: MatchingRules = MatchingRulesImpl(),
  var generators: Generators = Generators()
): HttpPart() {

  override fun toString() =
    "\tstatus: $status\n\theaders: $headers\n\tmatchers: $matchingRules\n\tgenerators: $generators\n\tbody: $body"

  fun copy() = Response(status, headers.toMutableMap(), body.copy(), matchingRules.copy(), generators.copy())

  fun generatedResponse(context: Map<String, Any> = mapOf(), mode: GeneratorTestMode = GeneratorTestMode.Provider): Response {
    val r = this.copy()
    generators.applyGenerator(Category.STATUS, mode) { _, g -> r.status = g.generate(context) as Int }
    generators.applyGenerator(Category.HEADER, mode) { key, g ->
      r.headers[key] = listOf(g.generate(context).toString())
    }
    r.body = generators.applyBodyGenerators(r.body, ContentType(mimeType()), context, mode)
    return r
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Response

    if (status != other.status) return false
    if (headers != other.headers) return false
    if (body != other.body) return false
    if (matchingRules != other.matchingRules) return false
    if (generators != other.generators) return false

    return true
  }

  override fun hashCode(): Int {
    var result = status
    result = 31 * result + headers.hashCode()
    result = 31 * result + body.hashCode()
    result = 31 * result + matchingRules.hashCode()
    result = 31 * result + generators.hashCode()
    return result
  }

  companion object: KLogging() {
    const val DEFAULT_STATUS = 200

    @JvmStatic
    fun fromMap(map: Map<String, Any>): Response {
      val status = map.getOrDefault("status", DEFAULT_STATUS) as Int
      val headers = (map.getOrDefault("headers", mutableMapOf<String, Any>()) as Map<String, Any>)
        .entries.associate { (key, value) ->
        if (value is List<*>) {
          key to value as List<String>
        } else {
          key to value.toString().split(",").map { it.trim() }
        }
      }
      val body = if (map.containsKey("body"))
        OptionalBody.body(map["body"]?.toString()?.toByteArray())
        else OptionalBody.missing()
      val matchingRules = MatchingRulesImpl.fromMap(map["matchingRules"] as Map<String, Map<String, Any>>)
      val generators = Generators.fromMap(map["generators"] as Map<String, Map<String, Any>>)
      return Response(status, headers.toMutableMap(), body, matchingRules, generators)
    }
  }
}
