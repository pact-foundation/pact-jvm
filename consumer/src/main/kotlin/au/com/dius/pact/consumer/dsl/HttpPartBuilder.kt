package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.core.model.IHttpPart
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.support.isNotEmpty
import io.ktor.http.HeaderValue
import io.ktor.http.parseHeaderValue

abstract class HttpPartBuilder(private val part: IHttpPart) {

  /**
   * Adds a header to the HTTP part. The value will be converted to a string (using the toString() method), unless it
   * is a List. With a List as the value, it will set up a multiple value header. If the value resolves to a single
   * String, and the header key is for a header that has multiple values, the values will be split into a list.
   *
   * For example: `header("OPTIONS", "GET, POST, PUT")` is the same as `header("OPTIONS", List.of("GET", "POST, "PUT"))`
   */
  open fun header(key: String, value: Any): HttpPartBuilder {
    val headValues = when (value) {
      is List<*> -> value.mapIndexed { index, v ->
        if (v is Matcher) {
          if (v.matcher != null) {
            part.matchingRules.addCategory("header").addRule("$key[$index]", v.matcher!!)
          }
          if (v.generator != null) {
            part.generators.addGenerator(Category.HEADER, "$key[$index]", v.generator!!)
          }
          v.value.toString()
        } else {
          v.toString()
        }
      }
      is Matcher -> {
        if (value.matcher != null) {
          part.matchingRules.addCategory("header").addRule(key, value.matcher!!)
        }
        if (value.generator != null) {
          part.generators.addGenerator(Category.HEADER, key, value.generator!!)
        }
        listOf(value.value.toString())
      }
      else -> if (isKnowSingleValueHeader(key)) {
        listOf(value.toString())
      } else {
        parseHeaderValue(value.toString()).map { headerToString(it) }
      }
    }
    part.headers[key] = headValues
    return this
  }

  /**
   * Adds all the headers to the HTTP part. The values will be converted to a string (using the toString() method),
   * and the header key is for a header that has multiple values, the values will be split into a list.
   *
   * For example: `headers("OPTIONS", "GET, POST, PUT")` is the same as
   * `header("OPTIONS", List.of("GET", "POST, "PUT"))`
   */
  open fun headers(key: String, value: String, nameValuePairs: Array<out String>): HttpPartBuilder {
    require(nameValuePairs.size % 2 == 0) {
      "Pairs of key/values should be provided, but there is one key without a value."
    }

    val headValue = if (isKnowSingleValueHeader(key)) {
      mutableListOf(value)
    } else {
      parseHeaderValue(value).map { headerToString(it) }.toMutableList()
    }
    val headersMap = nameValuePairs.toList().chunked(2).fold(mutableMapOf(key to headValue)) { acc, values ->
      val k = values[0]
      val v = if (isKnowSingleValueHeader(k)) {
        listOf(values[1])
      } else {
        parseHeaderValue(values[1]).map { headerToString(it) }
      }
      if (acc.containsKey(k)) {
        acc[k]!!.addAll(v)
      } else {
        acc[k] = v.toMutableList()
      }
      acc
    }

    part.headers.putAll(headersMap)

    return this
  }

  /**
   * Adds all the headers to the HTTP part. The values will be converted to a string (using the toString() method),
   * and the header key is for a header that has multiple values, the values will be split into a list.
   *
   * For example: `headers("OPTIONS", "GET, POST, PUT")` is the same as
   * `header("OPTIONS", List.of("GET", "POST, "PUT"))`
   */
  open fun headers(nameValuePairs: Array<out Pair<String, Any>>): HttpPartBuilder {
    val headersMap = nameValuePairs.toList().fold(mutableMapOf<String, MutableList<String>>()) { acc, value ->
      val k = value.first
      val v = if (value.second is Matcher) {
        val matcher = value.second as Matcher
        if (matcher.matcher != null) {
          part.matchingRules.addCategory("header").addRule(k, matcher.matcher!!)
        }
        if (matcher.generator != null) {
          part.generators.addGenerator(Category.HEADER, k, matcher.generator!!)
        }
        listOf(matcher.value.toString())
      } else if (isKnowSingleValueHeader(k)) {
        listOf(value.second.toString())
      } else {
        parseHeaderValue(value.second.toString()).map { headerToString(it) }
      }
      if (acc.containsKey(k)) {
        acc[k]!!.addAll(v)
      } else {
        acc[k] = v.toMutableList()
      }
      acc
    }

    part.headers.putAll(headersMap)

    return this
  }

  /**
   * Adds all the headers to the HTTP part. You can either pass a Map<String -> String>, and values will be converted
   * to a string (using the toString() method), and the header key is for a header that has multiple values,
   * the values will be split into a list.
   *
   * For example: `headers(Map<"OPTIONS", "GET, POST, PUT">)` is the same as
   * `header(Map<"OPTIONS", List<"GET", "POST, "PUT">>))`
   *
   * Or pass in a Map<String -> List<String>> and no conversion will take place.
   */
  open fun headers(values: Map<String, Any>): HttpPartBuilder {
    val headersMap = values.mapValues { entry ->
      val k = entry.key
      if (entry.value is Matcher) {
        val matcher = entry.value as Matcher
        if (matcher.matcher != null) {
          part.matchingRules.addCategory("header").addRule(k, matcher.matcher!!)
        }
        if (matcher.generator != null) {
          part.generators.addGenerator(Category.HEADER, k, matcher.generator!!)
        }
        listOf(matcher.value.toString())
      } else if (entry.value is List<*>) {
        (entry.value as List<*>).mapIndexed { index, v ->
          if (v is Matcher) {
            if (v.matcher != null) {
              part.matchingRules.addCategory("header").addRule("$k[$index]", v.matcher!!)
            }
            if (v.generator != null) {
              part.generators.addGenerator(Category.HEADER, "$k[$index]", v.generator!!)
            }
            v.value.toString()
          } else {
            v.toString()
          }
        }
      } else if (isKnowSingleValueHeader(k)) {
        listOf(entry.value.toString())
      } else {
        parseHeaderValue(entry.value.toString()).map { headerToString(it) }
      }
    }

    part.headers.putAll(headersMap)

    return this
  }

  /**
   * Sets the body of the HTTP part as a string value. If the content type is not already set, it will try to detect
   * the content type from the given string, otherwise will default to text/plain.
   */
  open fun body(body: String) = body(body, null)

  /**
   * Sets the body of the HTTP part as a string value. If the content type is not already set, it will try to detect
   * the content type from the given string, otherwise will default to text/plain.
   */
  open fun body(body: String, contentTypeString: String?): HttpPartBuilder {
    val contentTypeHeader = part.contentTypeHeader()
    val contentType = if (!contentTypeString.isNullOrEmpty()) {
      ContentType.fromString(contentTypeString)
    } else if (contentTypeHeader != null) {
      ContentType.fromString(contentTypeHeader)
    } else {
      OptionalBody.detectContentTypeInByteArray(body.toByteArray()) ?: ContentType.TEXT_PLAIN
    }

    val charset = contentType.asCharset()
    part.body = OptionalBody.body(body.toByteArray(charset), contentType)

    if (contentTypeHeader == null || contentTypeString.isNotEmpty()) {
      part.headers["content-type"] = listOf(contentType.toString())
    }

    return this
  }

  private fun isKnowSingleValueHeader(key: String): Boolean {
    return SINGLE_VALUE_HEADERS.contains(key.lowercase())
  }

  private fun headerToString(headerValue: HeaderValue): String {
    return if (headerValue.params.isNotEmpty()) {
      val params = headerValue.params.joinToString(";") { "${it.name}=${it.value}" }
      "${headerValue.value};$params"
    } else {
      headerValue.value
    }
  }

  companion object {
    val SINGLE_VALUE_HEADERS = setOf("date")
  }
}
