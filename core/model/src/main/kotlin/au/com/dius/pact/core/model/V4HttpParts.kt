package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.GeneratorTestMode
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

data class HttpRequest @JvmOverloads constructor(
  override val method: String = "GET",
  override val path: String = "/",
  override val query: Map<String, List<String>> = mapOf(),
  override val headers: Map<String, List<String>> = mapOf(),
  override val body: OptionalBody = OptionalBody.missing(),
  override val matchingRules: MatchingRules = MatchingRulesImpl(),
  override val generators: Generators = Generators()
): IRequest {
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

  override fun cookies(): List<String> {
    val cookieEntry = headers.entries.find { (k, _) -> k.toLowerCase() == Request.COOKIE_KEY }
    return if (cookieEntry != null) {
      cookieEntry.value.flatMap {
        it.split(';')
      }.map { it.trim() }
    } else {
      emptyList()
    }
  }

  override fun asHttpPart() = toV3Request()

  override fun headersWithoutCookie(): Map<String, List<String>> {
    return headers.filter { (k, _) -> k.toLowerCase() != Request.COOKIE_KEY }
  }

  override fun generatedRequest(context: MutableMap<String, Any>, mode: GeneratorTestMode): IRequest {
    return toV3Request().generatedRequest(context, mode)
  }

  override fun isMultipartFileUpload() = asHttpPart().isMultipartFileUpload()

  companion object {
    @JvmStatic
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

data class HttpResponse @JvmOverloads constructor(
  override val status: Int = 200,
  override val headers: Map<String, List<String>> = mapOf(),
  override val body: OptionalBody = OptionalBody.missing(),
  override val matchingRules: MatchingRules = MatchingRulesImpl(),
  override val generators: Generators = Generators()
) : IResponse {
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

  override fun generatedResponse(context: MutableMap<String, Any>, mode: GeneratorTestMode): IResponse {
    return this.toV3Response().generatedResponse(context, mode)
  }

  override fun asHttpPart() = toV3Response()

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
