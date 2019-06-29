package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import com.google.gson.JsonParser
import mu.KLogging
import java.net.URLEncoder

/**
 * Interaction between a consumer and a provider
 */
open class RequestResponseInteraction @JvmOverloads constructor(
  override val description: String,
  override val providerStates: List<ProviderState> = listOf(),
  val request: Request = Request(),
  val response: Response = Response(),
  override val interactionId: String? = null
) : Interaction {

  override fun toString() =
    "Interaction: $description\n\tin states ${displayState()}\nrequest:\n$request\n\nresponse:\n$response"

  fun displayState(): String {
    return if (providerStates.isEmpty() || providerStates.size == 1 && providerStates[0].name.isEmpty()) {
      "None"
    } else {
      providerStates.joinToString(COMMA) { it.name }
    }
  }

  override fun conflictsWith(other: Interaction) = other !is RequestResponseInteraction

  override fun uniqueKey() = "${displayState()}_$description"

  override fun toMap(pactSpecVersion: PactSpecVersion): Map<String, Any> {
    val interactionJson = mutableMapOf(
      "description" to description,
      "request" to requestToMap(request, pactSpecVersion),
      "response" to responseToMap(response, pactSpecVersion)
    )
    if (pactSpecVersion < PactSpecVersion.V3 && providerStates.isNotEmpty()) {
      interactionJson["providerState"] = providerStates.first().name
    } else if (providerStates.isNotEmpty()) {
      interactionJson["providerStates"] = providerStates.map { it.toMap() }
    }
    return interactionJson
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RequestResponseInteraction

    if (description != other.description) return false
    if (providerStates != other.providerStates) return false
    if (request != other.request) return false
    if (response != other.response) return false

    return true
  }

  override fun hashCode(): Int {
    var result = description.hashCode()
    result = 31 * result + providerStates.hashCode()
    result = 31 * result + request.hashCode()
    result = 31 * result + response.hashCode()
    return result
  }

  companion object : KLogging() {
    const val COMMA = ", "

    fun requestToMap(request: Request, pactSpecVersion: PactSpecVersion): Map<String, Any?> {
      val map = mutableMapOf<String, Any?>(
        "method" to request.method.toUpperCase(),
        "path" to request.path
      )
      if (request.headers.isNotEmpty()) {
        map["headers"] = request.headers.entries.associate { (key, value) -> key to value.joinToString(COMMA) }
      }
      if (request.query.isNotEmpty()) {
        map["query"] = if (pactSpecVersion >= PactSpecVersion.V3) request.query else mapToQueryStr(request.query)
      }
      if (request.body.isPresent()) {
        map["body"] = parseBody(request)
      }
      if (request.matchingRules.isNotEmpty()) {
        map["matchingRules"] = request.matchingRules.toMap(pactSpecVersion)
      }
      if (request.generators.isNotEmpty() && pactSpecVersion >= PactSpecVersion.V3) {
        map["generators"] = request.generators.toMap(pactSpecVersion)
      }

      return map
    }

    fun responseToMap(response: Response, pactSpecVersion: PactSpecVersion): Map<String, Any?> {
      val map = mutableMapOf<String, Any?>("status" to response.status)
      if (response.headers.isNotEmpty()) {
        map["headers"] = response.headers.entries.associate { (key, value) -> key to value.joinToString(COMMA) }
      }
      if (response.body.isPresent()) {
        map["body"] = parseBody(response)
      }
      if (response.matchingRules.isNotEmpty()) {
        map["matchingRules"] = response.matchingRules.toMap(pactSpecVersion)
      }
      if (response.generators.isNotEmpty() && pactSpecVersion >= PactSpecVersion.V3) {
        map["generators"] = response.generators.toMap(pactSpecVersion)
      }
      return map
    }

    private fun mapToQueryStr(query: Map<String, List<String>>): String {
      return query.entries.joinToString("&") { (k, v) ->
        v.joinToString { "$k=${URLEncoder.encode(it, "UTF-8")}" }
      }
    }

    private fun parseBody(httpPart: HttpPart): Any? {
      return if (httpPart.jsonBody() && httpPart.body.isPresent()) {
        val body = Json.fromJson(JsonParser().parse(httpPart.body.valueAsString()))
        if (body is String) {
          httpPart.body.valueAsString()
        } else {
          body
        }
      } else {
        httpPart.body.valueAsString()
      }
    }
  }
}
