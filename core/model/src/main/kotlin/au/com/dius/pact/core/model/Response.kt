package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
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
) : HttpPart() {

  override fun toString() =
    "\tstatus: $status\n\theaders: $headers\n\tmatchers: $matchingRules\n\tgenerators: $generators\n\tbody: $body"

  fun copy() = Response(status, headers.toMutableMap(), body.copy(), matchingRules.copy(), generators.copy())

  fun generatedResponse(
    context: Map<String, Any> = mapOf(),
    mode: GeneratorTestMode = GeneratorTestMode.Provider
  ): Response {
    val r = this.copy()
    generators.applyGenerator(Category.STATUS, mode) { _, g -> r.status = g.generate(context) as Int }
    generators.applyGenerator(Category.HEADER, mode) { key, g ->
      r.headers[key] = listOf(g.generate(context).toString())
    }
    r.body = generators.applyBodyGenerators(r.body, ContentType.fromString(contentType()), context, mode)
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

  companion object : KLogging() {
    const val DEFAULT_STATUS = 200

    @JvmStatic
    fun fromJson(json: JsonValue.Object): Response {
      val status = when {
        json.has("status") -> {
          val statusJson = json["status"]
          when {
            statusJson.isNumber -> statusJson.asNumber().toInt()
            statusJson is JsonValue.StringValue -> statusJson.asString().toInt()
            else -> DEFAULT_STATUS
          }
        }
        else -> DEFAULT_STATUS
      }
      val headers = if (json.has("headers") && json["headers"] is JsonValue.Object) {
        json["headers"].asObject().entries.entries.associate { (key, value) ->
          if (value is JsonValue.Array) {
            key to value.values.map { Json.toString(it) }
          } else {
            key to Json.toString(value).split(",").map { it.trim() }
          }
        }
      } else {
        emptyMap()
      }

      var contentType = ContentType.JSON
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
      return Response(status, headers.toMutableMap(), body, matchingRules, generators)
    }
  }
}
