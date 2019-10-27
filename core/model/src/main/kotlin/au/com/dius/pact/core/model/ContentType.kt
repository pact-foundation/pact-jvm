package au.com.dius.pact.core.model

import mu.KLogging
import java.nio.charset.Charset

private val jsonRegex = Regex("application\\/.*json")
private val xmlRegex = Regex("application\\/.*xml")

data class ContentType(val contentType: String) {

  private val parsedContentType: org.apache.http.entity.ContentType? = try {
    if (contentType.isNotEmpty()) {
      org.apache.http.entity.ContentType.parse(contentType)
    } else {
      null
    }
  } catch (e: Exception) {
    logger.debug { "Failed to parse content type '$contentType'" }
    null
  }

  fun isJson(): Boolean = jsonRegex.matches(contentType.toLowerCase())

  fun isXml(): Boolean = xmlRegex.matches(contentType.toLowerCase())

  override fun toString() = contentType

  fun asCharset(): Charset = parsedContentType?.charset ?: Charset.defaultCharset()

  fun asMimeType() = parsedContentType?.mimeType ?: contentType

  companion object : KLogging() {
    val UNKNOWN = ContentType("")
    val TEXT_PLAIN = ContentType("text/plain")
    val JSON = ContentType("application/json")
  }
}
