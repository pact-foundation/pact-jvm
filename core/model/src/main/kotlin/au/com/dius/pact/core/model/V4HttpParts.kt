package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue

private fun headersFromJson(json: JsonValue): Map<String, List<String>> {
  return if (json.has("headers") && json["headers"] is JsonValue.Object) {
    json["headers"].asObject()!!.entries.entries.associate { (key, value) ->
      if (value is JsonValue.Array) {
        key to value.values.map { Json.toString(it) }
      } else {
        key to Json.toString(value).split(",").map { it.trim() }
      }
    }
  } else {
    emptyMap()
  }
}

data class HttpRequest(
  val method: String,
  val path: String,
  val query: Map<String, List<String>>,
  val headers: Map<String, List<String>>,
  val body: OptionalBody,
  val matchingRules: MatchingRules,
  val generators: Generators
) {
  fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(matchingRules.validateForVersion(pactVersion))
    errors.addAll(generators.validateForVersion(pactVersion))
    return errors
  }

  fun toV3Request(): Request {
    return Request(method, path, query.toMutableMap(), headers.toMutableMap(), body, matchingRules, generators)
  }

  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "method" to method.toUpperCase(),
      "path" to path
    )
    if (headers.isNotEmpty()) {
      map["headers"] = headers
    }
    if (query.isNotEmpty()) {
      map["query"] = query
    }
    if (body.isPresent()) {
      map["body"] = body.toV4Format()
    }
    if (matchingRules.isNotEmpty()) {
      map["matchingRules"] = matchingRules.toMap(PactSpecVersion.V4)
    }
    if (generators.isNotEmpty()) {
      map["generators"] = generators.toMap(PactSpecVersion.V4)
    }

    return map
  }

  companion object {
    fun fromJson(json: JsonValue): HttpRequest {
      val method = if (json.has("method")) Json.toString(json["method"]).toUpperCase() else Request.DEFAULT_METHOD
      val path = if (json.has("path")) Json.toString(json["path"]) else Request.DEFAULT_PATH
      val query = BaseRequest.parseQueryParametersToMap(json["query"])
      val headers = headersFromJson(json)
      val body = bodyFromJson("body", json, headers)
      val matchingRules = if (json.has("matchingRules") && json["matchingRules"] is JsonValue.Object)
        MatchingRulesImpl.fromJson(json["matchingRules"])
      else MatchingRulesImpl()
      val generators = if (json.has("generators") && json["generators"] is JsonValue.Object)
        Generators.fromJson(json["generators"])
      else Generators()

      return HttpRequest(method, path, query, headers, body, matchingRules, generators)
    }
  }
}

data class HttpResponse(
  val status: Int,
  val headers: Map<String, List<String>>,
  val body: OptionalBody,
  val matchingRules: MatchingRules,
  val generators: Generators
) {
  fun validateForVersion(pactVersion: PactSpecVersion): List<String> {
    val errors = mutableListOf<String>()
    errors.addAll(matchingRules.validateForVersion(pactVersion))
    errors.addAll(generators.validateForVersion(pactVersion))
    return errors
  }

  fun toV3Response(): Response {
    return Response(status, headers.toMutableMap(), body, matchingRules, generators)
  }

  fun toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>("status" to status)
    if (headers.isNotEmpty()) {
      map["headers"] = headers
    }
    if (body.isPresent()) {
      map["body"] = body.toV4Format()
    }
    if (matchingRules.isNotEmpty()) {
      map["matchingRules"] = matchingRules.toMap(PactSpecVersion.V4)
    }
    if (generators.isNotEmpty()) {
      map["generators"] = generators.toMap(PactSpecVersion.V4)
    }
    return map
  }

  companion object {
    fun fromJson(json: JsonValue): HttpResponse {
      val status = when {
        json.has("status") -> {
          val statusJson = json["status"]
          when {
            statusJson.isNumber -> statusJson.asNumber()!!.toInt()
            statusJson is JsonValue.StringValue -> statusJson.asString()?.toInt() ?: Response.DEFAULT_STATUS
            else -> Response.DEFAULT_STATUS
          }
        }
        else -> Response.DEFAULT_STATUS
      }
      val headers = headersFromJson(json)
      val body = bodyFromJson("body", json, headers)
      val matchingRules = if (json.has("matchingRules") && json["matchingRules"] is JsonValue.Object)
        MatchingRulesImpl.fromJson(json["matchingRules"])
      else MatchingRulesImpl()
      val generators = if (json.has("generators") && json["generators"] is JsonValue.Object)
        Generators.fromJson(json["generators"])
      else Generators()
      return HttpResponse(status, headers, body, matchingRules, generators)
    }
  }
}
