package au.com.dius.pact.core.model

import java.nio.charset.Charset

private val jsonRegex = Regex("application\\/.*json")
private val xmlRegex = Regex("application\\/.*xml")

data class ContentType(val contentType: String) {

  fun isJson(): Boolean = jsonRegex.matches(contentType.toLowerCase())

  fun isXml(): Boolean = xmlRegex.matches(contentType.toLowerCase())

  override fun toString() = contentType

  fun asCharset(): Charset? {
    return try {
      org.apache.http.entity.ContentType.parse(contentType)?.charset
    } catch (e: Exception) {
      HttpPart.logger.debug { "Failed to parse content type '$contentType'" }
      null
    }
  }

  companion object {
    val UNKNOWN = ContentType("")
    val TEXT_PLAIN = ContentType("text/plain")
    val JSON = ContentType("application/json")
  }
}
