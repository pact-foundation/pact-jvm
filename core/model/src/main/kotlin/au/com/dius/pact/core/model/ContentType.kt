package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.isNotEmpty
import mu.KLogging
import org.apache.tika.mime.MediaType
import java.nio.charset.Charset

private val jsonRegex = Regex(".*json")
private val xmlRegex = Regex(".*xml")

data class ContentType(val contentType: MediaType?) {

  constructor(contentType: String) : this(MediaType.parse(contentType))

  fun isJson(): Boolean = if (contentType != null) jsonRegex.matches(contentType.subtype.toLowerCase()) else false

  fun isXml(): Boolean = if (contentType != null) xmlRegex.matches(contentType.subtype.toLowerCase()) else false

  fun isOctetStream(): Boolean = if (contentType != null)
    contentType.baseType.toString() == "application/octet-stream"
    else false

  override fun toString() = contentType.toString()

  fun asString() = contentType?.toString()

  fun asCharset(): Charset {
    return if (contentType != null && contentType.hasParameters()) {
      val cs = contentType.parameters["charset"]
      if (cs.isNotEmpty()) {
        Charset.forName(cs)
      } else {
        Charset.defaultCharset()
      }
    } else {
      Charset.defaultCharset()
    }
  }

  fun or(other: ContentType) = if (contentType == null) {
    other
  } else {
    this
  }

  fun getBaseType() = contentType?.baseType?.toString()

  companion object : KLogging() {
    @JvmStatic
    fun fromString(contentType: String?) = if (contentType.isNullOrEmpty()) {
      UNKNOWN
    } else {
      ContentType(contentType)
    }

    val XMLREGEXP = """^\s*<\?xml\s*version.*""".toRegex()
    val HTMLREGEXP = """^\s*(<!DOCTYPE)|(<HTML>).*""".toRegex()
    val JSONREGEXP = """^\s*(true|false|null|[0-9]+|"\w*|\{\s*(}|"\w+)|\[\s*).*""".toRegex()
    val XMLREGEXP2 = """^\s*<\w+\s*(:\w+=[\"”][^\"”]+[\"”])?.*""".toRegex()

    @JvmStatic
    val UNKNOWN = ContentType(null)
    @JvmStatic
    val TEXT_PLAIN = ContentType("text/plain")
    @JvmStatic
    val HTML = ContentType("text/html")
    @JvmStatic
    val JSON = ContentType("application/json")
    @JvmStatic
    val XML = ContentType("application/xml")
  }
}
