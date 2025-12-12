package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.isNotEmpty
import io.github.oshai.kotlinlogging.KLogging
import org.apache.tika.mime.MediaType
import org.apache.tika.mime.MediaTypeRegistry
import org.apache.tika.mime.MimeTypes
import java.nio.charset.Charset

private val jsonRegex = Regex(".*json")
private val xmlRegex = Regex(".*xml")

class ContentType(val contentType: MediaType?) {

  constructor(contentType: String) : this(MediaType.parse(contentType))

  fun isJson(): Boolean {
    return if (contentType != null) {
      when (System.getProperty("pact.content_type.override.${contentType.baseType}")) {
        "json" -> true
        else -> {
          if ("vnd.schemaregistry.v1+json" == contentType.subtype)
            false
          else if (jsonRegex.matches(contentType.subtype.toLowerCase())) {
            true
          } else {
            val superType = resolvedSuperType()
            superType != null && superType.type == "application" && superType.subtype == "json"
          }
        }
      }
    } else false
  }

  fun isXml(): Boolean = if (contentType != null) {
    when (System.getProperty("pact.content_type.override.${contentType.baseType}")) {
      "xml" -> true
      else -> xmlRegex.matches(contentType.subtype.toLowerCase())
    }
  } else false

  fun isKafkaSchemaRegistryJson(): Boolean = if (contentType != null) {
    when (System.getProperty("pact.content_type.override.${contentType.baseType}")) {
      "kafkaSchemaRegistryJson" -> true
      else -> contentType.subtype == "vnd.schemaregistry.v1+json"
    }
  } else false

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

  @Suppress("ComplexMethod")
  fun isBinaryType(): Boolean {
    return if (contentType != null) {
      val superType = resolvedSuperType() ?: MediaType.OCTET_STREAM
      val type = contentType.type
      val baseType = superType.type
      val override = System.getProperty("pact.content_type.override.$type.${contentType.subtype}")
        ?: System.getProperty("pact.content_type.override.$type/${contentType.subtype}")
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
        type == "multipart" -> true
        else -> false
      }
    } else false
  }

  private fun resolvedSuperType(): MediaType? {
    if (contentType == null) return null
    val asString = contentType.toString()
    val mapped = Companion.customSuperTypes[asString]
    if (mapped != null) return MediaType.parse(mapped)
    val supertype = registry.getSupertype(contentType)
    return if (supertype != null && supertype.type == "text" && supertype.subtype == "javascript") {
      MediaType.parse("application/javascript")
    } else supertype
  }

  fun isMultipart() = if (contentType != null)
    contentType.baseType.type == "multipart"
    else false

  fun isMultipartFormData() = isMultipart() && contentType?.subtype == "form-data"

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

  fun getSupertype() : ContentType? {
    return if (contentType != null && contentType.subtype.endsWith("+json")) {
      JSON
    } else if (contentType != null) {
      // Respect custom mappings for some known types that may not be present
      // in the Tika registry for all versions.
      val asString = contentType.toString()
      val mapped = customSuperTypes[asString]
      if (mapped != null) {
        ContentType(mapped)
      } else {
        val supertype = registry.getSupertype(contentType)
        if (supertype != null) {
          // Normalise some Tika results to the values expected by the tests
          if (supertype.type == "text" && supertype.subtype == "javascript") {
            ContentType("application/javascript")
          } else {
            ContentType(supertype)
          }
        } else {
          null
        }
      }
    } else null
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

    val registry:
      MediaTypeRegistry = MimeTypes.getDefaultMimeTypes(ContentType::class.java.classLoader)
      .mediaTypeRegistry

    val customSuperTypes = mapOf(
      "application/graphql" to "application/json",
      "application/x-www-form-urlencoded" to "text/plain",
      "application/protobuf" to "application/octet-stream"
    )

    @JvmStatic
    val UNKNOWN = ContentType(null)
    @JvmStatic
    val TEXT_PLAIN = ContentType("text/plain; charset=ISO-8859-1")
    @JvmStatic
    val OCTET_STEAM = ContentType("application/octet-stream")
    @JvmStatic
    val HTML = ContentType("text/html")
    @JvmStatic
    val JSON = ContentType("application/json")
    @JvmStatic
    val XML = ContentType("application/xml")
    @JvmStatic
    val KAFKA_SCHEMA_REGISTRY_JSON = ContentType("application/vnd.schemaregistry.v1+json")
  }
}
