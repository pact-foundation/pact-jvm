package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.isNotEmpty
import mu.KLogging
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MediaTypeRegistry
import org.apache.tika.mime.MimeTypes
import java.nio.charset.Charset

private val jsonRegex = Regex(".*json")
private val xmlRegex = Regex(".*xml")

class ContentType(val contentType: MediaType?) {

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

  fun isBinaryType(): Boolean {
    return if (contentType != null) {
      val superType = registry.getSupertype(contentType) ?: MediaType.OCTET_STREAM
      val type = contentType.type
      val baseType = superType.type
      val override = System.getProperty("pact.content_type.override.$type.${contentType.subtype}")
      when {
        override.isNotEmpty() -> override == "binary"
        type == "text" || baseType == "text" -> false
        type == "image" || baseType == "image" -> true
        type == "audio" || baseType == "audio" -> true
        type == "video" || baseType == "video" -> true
        type == "application" && contentType.subtype == "pdf" -> true
        type == "application" && contentType.subtype == "xml" -> false
        type == "application" && contentType.subtype == "json" -> false
        type == "application" && superType.subtype == "javascript" -> false
        type == "application" && contentType.subtype.matches(JSON_TYPE) -> false
        superType == MediaType.APPLICATION_ZIP -> true
        superType == MediaType.OCTET_STREAM -> true
        else -> false
      }
    } else false
  }

  fun isMultipart() = if (contentType != null)
    contentType.baseType.type == "multipart"
    else false

  override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other is MediaType -> contentType == other
      other !is ContentType -> false
      else -> contentType == other.contentType
    }
  }

  override fun hashCode(): Int {
    return contentType?.hashCode() ?: 0
  }

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

    val JSON_TYPE = ".*json".toRegex(setOf(RegexOption.IGNORE_CASE))

    val registry: MediaTypeRegistry = MimeTypes.getDefaultMimeTypes(ContentType::class.java.classLoader).mediaTypeRegistry

    @JvmStatic
    val UNKNOWN = ContentType(null)
    @JvmStatic
    val TEXT_PLAIN = ContentType("text/plain; charset=ISO-8859-1")
    @JvmStatic
    val HTML = ContentType("text/html")
    @JvmStatic
    val JSON = ContentType("application/json")
    @JvmStatic
    val XML = ContentType("application/xml")
  }
}
