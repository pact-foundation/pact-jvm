package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.generators.Generator
import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.model.generators.Generators
import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import io.github.oshai.kotlinlogging.KLogging
import org.apache.commons.beanutils.BeanUtils

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
  override var method: String = "GET",
  override var path: String = "/",
  override var query: MutableMap<String, List<String>> = mutableMapOf(),
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override val matchingRules: MatchingRules = MatchingRulesImpl(),
  override val generators: Generators = Generators()
): IRequest, IHttpPart, KLogging() {
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
    if (body.isPresent() || body.isEmpty()) {
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

  override fun copy(): IRequest = this.copy(body = this.body.copy(), matchingRules = this.matchingRules.copy(),
    generators = this.generators.copy())

  override fun hasHeader(name: String) = headers.any { (key, _) -> key.lowercase() == name }

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

      return HttpRequest(method, path, query.toMutableMap(), headers.toMutableMap(), body, matchingRules, generators)
    }
  }

  override fun setupGenerators(category: Category, context: Map<String, Any>): Map<String, Generator> {
    val generators = generators.categories[category] ?: emptyMap()
    val matchingRuleGenerators = matchingRules.rulesForCategory(category.name.lowercase()).generators(context)
    return generators + matchingRuleGenerators
  }

  @Suppress("ComplexMethod", "NestedBlockDepth")
  fun updateProperties(values: Map<String, Any?>) {
    logger.debug { "updateProperties(values=$values)" }
    values.forEach { (key, value) ->
      when (key) {
        "headers" -> when (value) {
          is Map<*, *> -> {
            headers = value
              .mapKeys { it.key.toString() }
              .mapValues { (_, headerValue) ->
              when (headerValue) {
                is List<*> -> headerValue.map { it.toString() }
                else -> listOf(headerValue.toString())
              }
            }.toMutableMap()
          }
          else -> throw IllegalArgumentException("$value is not a valid value for headers")
        }
        "query" -> when (value) {
          is Map<*, *> -> value.forEach { (name, queryValue) ->
              val queryName = name.toString()
              if (!query.containsKey(queryName)) {
                query[queryName] = mutableListOf()
              }
              when (queryValue) {
                is List<*> -> query[queryName] = query[queryName]!! + queryValue.map { it.toString() }
                else -> query[queryName] = query[queryName]!! + queryValue.toString()
              }
            }
          is String -> query.putAll(queryStringToMap(value.toString()))
          else -> throw IllegalArgumentException("$value is not a valid value for query parameters")
        }
        else -> BeanUtils.setProperty(this, key, value)
      }
    }
  }
}

data class HttpResponse @JvmOverloads constructor(
  override var status: Int = 200,
  override var headers: MutableMap<String, List<String>> = mutableMapOf(),
  override var body: OptionalBody = OptionalBody.missing(),
  override val matchingRules: MatchingRules = MatchingRulesImpl(),
  override val generators: Generators = Generators()
) : IResponse, IHttpPart {
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
    if (body.isPresent() || body.isEmpty()) {
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

  fun updateProperties(values: Map<String, Any?>) {
    V4Interaction.logger.debug { "updateProperties(values=$values)" }
    values.forEach { (key, value) ->
      when (key) {
        "headers" -> when (value) {
          is Map<*, *> -> {
            headers = value
              .mapKeys { it.key.toString() }
              .mapValues { (_, headerValue) ->
                when (headerValue) {
                  is List<*> -> headerValue.map { it.toString() }
                  else -> listOf(headerValue.toString())
                }
              }.toMutableMap()
          }
          else -> throw IllegalArgumentException("$value is not a valid value for headers")
        }
        else -> BeanUtils.setProperty(this, key, value)
      }
    }
  }

  override fun hasHeader(name: String) = headers.any { (key, _) -> key.lowercase() == name }

  override fun copyResponse() = this.copy()

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
      return HttpResponse(status, headers.toMutableMap(), body, matchingRules, generators)
    }
  }

  override fun setupGenerators(category: Category, context: Map<String, Any>): Map<String, Generator> {
    val generators = generators.categories[category] ?: emptyMap()
    val matchingRuleGenerators = matchingRules.rulesForCategory(category.name.lowercase()).generators(context)
    return generators + matchingRuleGenerators
  }
}
