package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * Response from a provider to a consumer
 */
interface IResponse: IHttpPart {
  var status: Int
  override val headers: MutableMap<String, List<String>>
  override var body: OptionalBody
  override val matchingRules: MatchingRules
  override val generators: Generators

  fun asHttpPart() : HttpPart

  /**
   * Make a copy of this response
   */
  fun copyResponse(): IResponse
}

/**
 * Response from a provider to a consumer
 */
class Response @JvmOverloads constructor(
  override var status: Int = DEFAULT_STATUS,
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override var matchingRules: MatchingRules = MatchingRulesImpl(),
  override var generators: Generators = Generators()
) : HttpPart(), IResponse {

  override fun toString() =
    "\tstatus: $status\n\theaders: $headers\n\tmatchers: $matchingRules\n\tgenerators: $generators\n\tbody: $body"

  override fun copyResponse() =
    Response(status, headers.toMutableMap(), body.copy(), matchingRules.copy(), generators.copy())

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

  fun asV4Response(): HttpResponse {
    return HttpResponse(status, headers, body, matchingRules, generators)
  }

  override fun asHttpPart() = this

  override fun hasHeader(name: String) = headers.any { (key, _) -> key.lowercase() == name }

  companion object {
    const val DEFAULT_STATUS = 200

    @JvmStatic
    fun fromJson(json: JsonValue.Object): Response {
      val status = statusFromJson(json)
      val headers = headersFromJson(json)

      var contentType = ContentType.UNKNOWN
      val contentTypeEntry = headers.entries.find { it.key.uppercase(Locale.getDefault()) == "CONTENT-TYPE" }
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

    private fun headersFromJson(json: JsonValue.Object) =
      if (json.has("headers") && json["headers"] is JsonValue.Object) {
        json["headers"].asObject()!!.entries.entries.associate { (key, value) ->
          key to HeaderParser.fromJson(key, value)
        }
      } else {
        emptyMap()
      }

    private fun statusFromJson(json: JsonValue.Object) = when {
      json.has("status") -> {
        val statusJson = json["status"]
        when {
          statusJson.isNumber -> statusJson.asNumber()!!.toInt()
          statusJson is JsonValue.StringValue -> statusJson.asString()?.toInt() ?: DEFAULT_STATUS
          else -> DEFAULT_STATUS
        }
      }
      else -> DEFAULT_STATUS
    }
  }
}
