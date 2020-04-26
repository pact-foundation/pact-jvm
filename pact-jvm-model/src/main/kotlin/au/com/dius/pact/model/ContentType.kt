package au.com.dius.pact.model

import mu.KLogging
import java.nio.charset.Charset

private val jsonRegex = Regex("application\\/.*json")
private val xmlRegex = Regex("application\\/.*xml")

data class ContentType(val contentType: String?) {

  private val parsedContentType: org.apache.http.entity.ContentType? = try {
    if (contentType.isNullOrEmpty()) {
      null
    } else {
      org.apache.http.entity.ContentType.parse(contentType)
    }
  } catch (e: Exception) {
    logger.debug { "Failed to parse content type '$contentType'" }
    null
  }

  fun isJson(): Boolean = if (contentType != null) jsonRegex.matches(contentType.toLowerCase()) else false

  fun isXml(): Boolean = if (contentType != null) xmlRegex.matches(contentType.toLowerCase()) else false

  override fun toString() = contentType.toString()

  fun asCharset(): Charset = parsedContentType?.charset ?: Charset.defaultCharset()

  fun asMimeType() = parsedContentType?.mimeType ?: contentType

  companion object : KLogging() {
    @JvmStatic
    val UNKNOWN = ContentType("")
    @JvmStatic
    val TEXT_PLAIN = ContentType("text/plain")
    @JvmStatic
    val HTML = ContentType("text/html")
    @JvmStatic
    val JSON = ContentType("application/json")
  }
}
