package au.com.dius.pact.consumer.dsl

import au.com.dius.pact.consumer.Headers.headerToString
import au.com.dius.pact.consumer.Headers.isKnowMultiValueHeader
import au.com.dius.pact.core.model.IHttpPart
import au.com.dius.pact.core.model.ContentType
import au.com.dius.pact.core.model.ContentType.Companion.JSON
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.generators.Category
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
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
      else -> if (isKnowMultiValueHeader(key)) {
        parseHeaderValue(value.toString()).map { headerToString(it) }
      } else {
        listOf(value.toString())
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

    val headValue = if (isKnowMultiValueHeader(key)) {
      parseHeaderValue(value).map { headerToString(it) }.toMutableList()
    } else {
      mutableListOf(value)
    }
    val headersMap = nameValuePairs.toList().chunked(2).fold(mutableMapOf(key to headValue)) { acc, values ->
      val k = values[0]
      val v = if (isKnowMultiValueHeader(k)) {
        parseHeaderValue(values[1]).map { headerToString(it) }
      } else {
        listOf(values[1])
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
      } else if (isKnowMultiValueHeader(k)) {
        parseHeaderValue(value.second.toString()).map { headerToString(it) }
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
      } else if (isKnowMultiValueHeader(k)) {
        parseHeaderValue(entry.value.toString()).map { headerToString(it) }
      } else {
        listOf(entry.value.toString())
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

  /**
   * Sets the body of the HTTP part as a byte array. If the content type is not already set, will default to
   * application/octet-stream.
   */
  open fun body(body: ByteArray) = body(body, null)

  /**
   * Sets the body of the HTTP part as a string value. If the content type is not provided or already set, will
   * default to application/octet-stream.
   */
  open fun body(body: ByteArray, contentTypeString: String?): HttpPartBuilder {
    val contentTypeHeader = part.contentTypeHeader()
    val contentType = if (!contentTypeString.isNullOrEmpty()) {
      ContentType.fromString(contentTypeString)
    } else if (contentTypeHeader != null) {
      ContentType.fromString(contentTypeHeader)
    } else {
      ContentType.OCTET_STEAM
    }

    part.body = OptionalBody.body(body, contentType)

    if (contentTypeHeader == null || contentTypeString.isNotEmpty()) {
      part.headers["content-type"] = listOf(contentType.toString())
    }

    return this
  }

  /**
   * Sets the body, content type and matching rules from a DslPart
   */
  open fun body(dslPart: DslPart): HttpPartBuilder {
    val parent = dslPart.close()!!

    part.matchingRules.addCategory(parent.matchers)
    part.generators.addGenerators(parent.generators)

    val contentTypeHeader = part.contentTypeHeader()
    if (contentTypeHeader.isNullOrEmpty()) {
      part.headers["content-type"] = listOf(JSON.toString())
      part.body = OptionalBody.body(parent.toString().toByteArray())
    } else {
      val ct = ContentType(contentTypeHeader)
      val charset = ct.asCharset()
      part.body = OptionalBody.body(parent.toString().toByteArray(charset), ct)
    }

    return this
  }

  /**
   * Sets the body, content type and matching rules from a BodyBuilder
   */
  open fun body(builder: BodyBuilder): HttpPartBuilder {
    part.matchingRules.addCategory(builder.matchers)
    val headerMatchers = builder.headerMatchers
    if (headerMatchers != null) {
      part.matchingRules.addCategory(headerMatchers)
    }

    part.generators.addGenerators(builder.generators)

    val contentTypeHeader = part.contentTypeHeader()
    val contentType = builder.contentType
    if (contentTypeHeader.isNullOrEmpty()) {
      part.headers["content-type"] = listOf(contentType.toString())
      part.body = OptionalBody.body(builder.buildBody(), contentType)
    } else {
      part.body = OptionalBody.body(builder.buildBody())
    }

    return this
  }

  /**
   * Sets up a content type matcher to match any body of the given content type
   */
  open fun bodyMatchingContentType(contentType: String, exampleContents: ByteArray): HttpPartBuilder {
    val ct = ContentType(contentType)
    part.body = OptionalBody.body(exampleContents, ct)
    part.headers["content-type"] = listOf(contentType)
    part.matchingRules.addCategory("body").addRule("$", ContentTypeMatcher(contentType))
    return this
  }

  /**
   * Sets up a content type matcher to match any body of the given content type
   */
  open fun bodyMatchingContentType(contentType: String, exampleContents: String): HttpPartBuilder {
    val ct = ContentType(contentType)
    return bodyMatchingContentType(contentType, exampleContents.toByteArray(ct.asCharset()))
  }
}
