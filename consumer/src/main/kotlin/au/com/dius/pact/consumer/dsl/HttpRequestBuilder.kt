package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.HttpRequest
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.queryStringToMap

/**
 * Pact HTTP Request builder DSL that supports V4 formatted Pact files
 */
open class HttpRequestBuilder(private val request: HttpRequest): HttpPartBuilder(request) {
  /**
   * Terminate the builder and return the request object
   */
  fun build(): HttpRequest {
    return request
  }

  /**
   * HTTP Method for the request.
   */
  fun method(method: String): HttpRequestBuilder {
    request.method = method
    return this
  }

  /**
   * Path for the request.
   */
  fun path(path: String): HttpRequestBuilder {
    request.path = path
    return this
  }

  /**
   * Sets the path of the request using a matching rule. For example:
   *
   * ```
   * path(regexp("\\/path\\/\\d+", "/path/1000"))
   * ```
   */
  fun path(matcher: Matcher): HttpRequestBuilder {
    if (matcher.matcher != null) {
      request.matchingRules.addCategory("path").addRule(matcher.matcher!!)
    }
    if (matcher.generator != null) {
      request.generators.addGenerator(Category.PATH, "", matcher.generator!!)
    }
    request.path = matcher.value.toString()
    return this
  }

  override fun header(key: String, value: Any): HttpRequestBuilder {
    return super.header(key, value) as HttpRequestBuilder
  }

  override fun headers(key: String, value: String, vararg nameValuePairs: String): HttpRequestBuilder {
    return super.headers(key, value, nameValuePairs) as HttpRequestBuilder
  }

  override fun headers(vararg nameValuePairs: Pair<String, Any>): HttpRequestBuilder {
    return super.headers(nameValuePairs) as HttpRequestBuilder
  }

  override fun headers(values: Map<String, Any>): HttpRequestBuilder {
    return super.headers(values) as HttpRequestBuilder
  }

  override fun body(body: String): HttpRequestBuilder {
    return super.body(body) as HttpRequestBuilder
  }

  override fun body(body: String, contentTypeString: String?): HttpRequestBuilder {
    return super.body(body, contentTypeString) as HttpRequestBuilder
  }

  override fun body(body: ByteArray): HttpRequestBuilder {
    return super.body(body) as HttpRequestBuilder
  }

  override fun body(body: ByteArray, contentTypeString: String?): HttpRequestBuilder {
    return super.body(body, contentTypeString) as HttpRequestBuilder
  }

  override fun body(dslPart: DslPart): HttpRequestBuilder {
    return super.body(dslPart) as HttpRequestBuilder
  }

  override fun body(builder: BodyBuilder): HttpRequestBuilder {
    return super.body(builder) as HttpRequestBuilder
  }

  override fun bodyMatchingContentType(contentType: String, exampleContents: ByteArray): HttpRequestBuilder {
    return super.bodyMatchingContentType(contentType, exampleContents) as HttpRequestBuilder
  }

  override fun bodyMatchingContentType(contentType: String, exampleContents: String): HttpRequestBuilder {
    return super.bodyMatchingContentType(contentType, exampleContents) as HttpRequestBuilder
  }

  /**
   * Adds a query parameter to the request. You can setup a multiple value query parameter by passing a List as the
   * value.
   */
  open fun queryParameter(key: String, value: Any): HttpRequestBuilder {
    val qValues = when (value) {
      is List<*> -> value.mapIndexed { index, v ->
        if (v is Matcher) {
          if (v.matcher != null) {
            request.matchingRules.addCategory("query").addRule("$key[$index]", v.matcher!!)
          }
          if (v.generator != null) {
            request.generators.addGenerator(Category.QUERY, "$key[$index]", v.generator!!)
          }
          v.value.toString()
        } else {
          v.toString()
        }
      }
      is Matcher -> {
        if (value.matcher != null) {
          request.matchingRules.addCategory("query").addRule(key, value.matcher!!)
        }
        if (value.generator != null) {
          request.generators.addGenerator(Category.QUERY, key, value.generator!!)
        }
        listOf(value.value.toString())
      }
      else -> listOf(value.toString())
    }
    request.query[key] = qValues
    return this
  }

  /**
   * Adds all the query parameters to the request.
   */
  open fun queryParameters(key: String, value: String, nameValuePairs: Array<out String>): HttpRequestBuilder {
    require(nameValuePairs.size % 2 == 0) {
      "Pairs of key/values should be provided, but there is one key without a value."
    }

    val qValue = mutableListOf(value)
    val queryMap = nameValuePairs.toList().chunked(2).fold(mutableMapOf(key to qValue)) { acc, values ->
      val k = values[0]
      val v = listOf(values[1])
      if (acc.containsKey(k)) {
        acc[k]!!.addAll(v)
      } else {
        acc[k] = v.toMutableList()
      }
      acc
    }

    request.query.putAll(queryMap)

    return this
  }

  /**
   * Adds all the query parameters to the request.
   */
  open fun queryParameters(nameValuePairs: Array<out Pair<String, Any>>): HttpRequestBuilder {
    val queryMap = nameValuePairs.toList().fold(mutableMapOf<String, MutableList<String>>()) { acc, value ->
      val k = value.first
      val v = if (value.second is Matcher) {
        val matcher = value.second as Matcher
        if (matcher.matcher != null) {
          request.matchingRules.addCategory("query").addRule(k, matcher.matcher!!)
        }
        if (matcher.generator != null) {
          request.generators.addGenerator(Category.QUERY, k, matcher.generator!!)
        }
        listOf(matcher.value.toString())
      } else {
        listOf(value.second.toString())
      }
      if (acc.containsKey(k)) {
        acc[k]!!.addAll(v)
      } else {
        acc[k] = v.toMutableList()
      }
      acc
    }

    request.query.putAll(queryMap)

    return this
  }

  /**
   * Adds all the query paraneters to the request. You can either pass a Map<String -> Object>, and values will be
   * converted to a string (using the toString() method), or pass in a Map<String -> List<Object>> multi-value
   * parameters.
   */
  open fun queryParameters(values: Map<String, Any>): HttpRequestBuilder {
    val queryMap = values.mapValues { entry ->
      val k = entry.key
      when (entry.value) {
        is Matcher -> {
          val matcher = entry.value as Matcher
          if (matcher.matcher != null) {
            request.matchingRules.addCategory("query").addRule(k, matcher.matcher!!)
          }
          if (matcher.generator != null) {
            request.generators.addGenerator(Category.QUERY, k, matcher.generator!!)
          }
          listOf(matcher.value.toString())
        }

        is List<*> -> {
          (entry.value as List<*>).mapIndexed { index, v ->
            if (v is Matcher) {
              if (v.matcher != null) {
                request.matchingRules.addCategory("query").addRule("$k[$index]", v.matcher!!)
              }
              if (v.generator != null) {
                request.generators.addGenerator(Category.QUERY, "$k[$index]", v.generator!!)
              }
              v.value.toString()
            } else {
              v.toString()
            }
          }
        }

        else -> listOf(entry.value.toString())
      }
    }

    request.query.putAll(queryMap)

    return this
  }

  /**
   * Adds all the query parameters to the request.
   */
  open fun queryParameters(query: String): HttpRequestBuilder {
    request.query.putAll(queryStringToMap(query))
    return this
  }
}
