package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.support.isNotEmpty
import mu.KLogging
import java.nio.charset.Charset

/**
 * Base trait for an object that represents part of an http message
 */
abstract class HttpPart {

  abstract var body: OptionalBody
  abstract var headers: MutableMap<String, List<String>>
  abstract var matchingRules: MatchingRules

  @Deprecated("use method that returns a content type object",
    replaceWith = ReplaceWith("determineContentType"))
  fun contentType(): String? = contentTypeHeader()?.split(Regex("\\s*;\\s*"))?.first()
    ?: body.contentType.asString()

  fun determineContentType(): ContentType {
    val headerValue = contentTypeHeader()?.split(Regex("\\s*;\\s*"))?.first()
    return if (headerValue.isNullOrEmpty())
      body.contentType
    else
      ContentType(headerValue)
  }

  fun contentTypeHeader(): String? {
    val contentTypeKey = headers.keys.find { CONTENT_TYPE.equals(it, ignoreCase = true) }
    return headers[contentTypeKey]?.first()
  }

  fun jsonBody() = determineContentType().isJson()

  fun xmlBody() = determineContentType().isXml()

  fun setDefaultContentType(contentType: String) {
    if (headers.keys.find { it.equals(CONTENT_TYPE, ignoreCase = true) } == null) {
      headers[CONTENT_TYPE] = listOf(contentType)
    }
  }

  fun charset(): Charset? {
    return when {
      body.isPresent() -> body.contentType.asCharset()
      else -> {
        val contentType = contentTypeHeader()
        if (contentType.isNotEmpty()) {
          ContentType(contentType!!).asCharset()
        } else {
          null
        }
      }
    }
  }

  companion object : KLogging() {
    private const val CONTENT_TYPE = "Content-Type"
  }
}
