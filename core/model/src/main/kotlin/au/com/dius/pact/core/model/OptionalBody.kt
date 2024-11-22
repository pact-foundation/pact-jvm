package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.ContentType.Companion.HTMLREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.JSONREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.UNKNOWN
import au.com.dius.pact.core.model.ContentType.Companion.XMLREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.XMLREGEXP2
import au.com.dius.pact.core.support.json.JsonParser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.apache.tika.config.TikaConfig
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import java.util.Base64

private val logger = KotlinLogging.logger {}

/**
 * If the content type should be overridden
 */
enum class ContentTypeHint {
  BINARY,
  TEXT,
  DEFAULT
}

/**
 * Class to represent missing, empty, null and present bodies
 */
data class OptionalBody @JvmOverloads constructor(
  val state: State,
  val value: ByteArray? = null,
  var contentType: ContentType = UNKNOWN,
  var contentTypeHint: ContentTypeHint = ContentTypeHint.DEFAULT
) {

  init {
    if (contentType == UNKNOWN) {
      val detectedContentType = detectContentType()
      if (detectedContentType != null) {
        this.contentType = detectedContentType
      }
    }
  }

  enum class State {
    MISSING, EMPTY, NULL, PRESENT
  }

  fun isMissing(): Boolean {
    return state == State.MISSING
  }

  fun isEmpty(): Boolean {
    return state == State.EMPTY
  }

  fun isNull(): Boolean {
    return state == State.NULL
  }

  fun isPresent(): Boolean {
    return state == State.PRESENT
  }

  fun isNotPresent(): Boolean {
    return state != State.PRESENT
  }

  fun orElse(defaultValue: ByteArray): ByteArray {
    return if (state == State.EMPTY || state == State.PRESENT) {
      this.value!!
    } else {
      defaultValue
    }
  }

  fun orEmpty() = orElse(ByteArray(0))

  fun unwrap(): ByteArray {
    if (isPresent() || isEmpty()) {
      return value!!
    } else {
      throw UnwrapMissingBodyException("Failed to unwrap value from a $state body")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OptionalBody

    if (state != other.state) return false
    if (value != null) {
      if (other.value == null) return false
      if (!value.contentEquals(other.value)) return false
    } else if (other.value != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = state.hashCode()
    result = 31 * result + (value?.contentHashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return when (state) {
      State.PRESENT -> if (contentTypeHint == ContentTypeHint.BINARY || contentType.isBinaryType()) {
        "PRESENT(${value!!.size} bytes starting with ${Hex.encodeHexString(slice(16))}...)"
      } else {
        "PRESENT(${value!!.toString(contentType.asCharset())})"
      }
      State.EMPTY -> "EMPTY"
      State.NULL -> "NULL"
      State.MISSING -> "MISSING"
    }
  }

  fun valueAsString(): String {
    return when (state) {
      State.PRESENT -> value!!.toString(contentType.asCharset())
      else -> ""
    }
  }

  fun detectContentType(): ContentType? = when {
    this.isPresent() -> {
      if (tika != null) {
        val metadata = Metadata()
        val mimetype = tika.detector.detect(TikaInputStream.get(value!!), metadata)
        if (mimetype.baseType.type == "text") {
          detectStandardTextContentType() ?: ContentType(mimetype)
        } else {
          ContentType(mimetype)
        }
      } else {
        detectStandardTextContentType()
      }
    }
    else -> null
  }

  fun detectStandardTextContentType(): ContentType? = when {
    isPresent() -> detectContentTypeInByteArray(value!!)
    else -> null
  }

  fun valueAsBase64(): String {
    return when (state) {
      State.PRESENT -> Base64.getEncoder().encodeToString(value!!)
      else -> ""
    }
  }

  fun slice(size: Int): ByteArray {
    return when (state) {
      State.PRESENT -> if (value!!.size > size) {
        value.copyOf(size)
      } else {
        value
      }
      else -> ByteArray(0)
    }
  }

  fun toV4Format(): Map<String, Any?> {
    return when (state) {
      State.PRESENT -> {
        if (value!!.isNotEmpty()) {
          if (contentType.isJson()) {
            if (contentTypeHint == ContentTypeHint.BINARY) {
              mapOf(
                "content" to valueAsString(),
                "contentType" to contentType.toString(),
                "encoded" to "JSON"
              )
            } else {
              mapOf(
                "content" to JsonParser.parseString(valueAsString()),
                "contentType" to contentType.toString(),
                "encoded" to false
              )
            }
          } else if (contentTypeHint == ContentTypeHint.BINARY || contentType.isBinaryType()) {
            mapOf(
              "content" to valueAsBase64(),
              "contentType" to contentType.toString(),
              "encoded" to "base64",
              "contentTypeHint" to contentTypeHint.name
            )
          } else {
            mapOf(
              "content" to valueAsString(),
              "contentType" to contentType.toString(),
              "encoded" to false,
              "contentTypeHint" to contentTypeHint.name
            )
          }
        } else {
          mapOf("content" to "")
        }
      }
      State.EMPTY -> mapOf("content" to "")
      else -> mapOf()
    }
  }

  companion object {

    @JvmStatic fun missing(): OptionalBody {
      return OptionalBody(State.MISSING)
    }

    @JvmStatic fun empty(): OptionalBody {
      return OptionalBody(State.EMPTY, ByteArray(0))
    }

    @JvmStatic fun nullBody(): OptionalBody {
      return OptionalBody(State.NULL)
    }

    @JvmStatic
    fun body(body: ByteArray?) = body(body, UNKNOWN, ContentTypeHint.DEFAULT)

    @JvmStatic
    fun body(body: ByteArray?, contentType: ContentType) = body(body, contentType, ContentTypeHint.DEFAULT)

    @JvmStatic
    @JvmOverloads
    fun body(body: String?, contentType: ContentType = UNKNOWN) =
      body(body?.toByteArray(), contentType, ContentTypeHint.DEFAULT)

    @JvmStatic
    fun body(
      body: ByteArray?,
      contentType: ContentType,
      contentTypeHint: ContentTypeHint
    ): OptionalBody {
      return when {
        body == null -> nullBody()
        body.isEmpty() -> empty()
        else -> OptionalBody(State.PRESENT, body, contentType, contentTypeHint)
      }
    }

    @Suppress("TooGenericExceptionCaught")
    private val tika = try { TikaConfig() } catch (e: RuntimeException) {
      logger.warn(e) { "Could not initialise Tika, detecting content types will be disabled" }
      null
    }

    fun detectContentTypeInByteArray(value: ByteArray): ContentType? {
      val newLine = '\n'.code.toByte()
      val cReturn = '\r'.code.toByte()
      val s = value.take(32).map {
        if (it == newLine || it == cReturn) ' ' else it.toInt().toChar()
      }.joinToString("")
      return when {
        s.matches(XMLREGEXP) -> ContentType.XML
        s.uppercase().matches(HTMLREGEXP) -> ContentType.HTML
        s.matches(JSONREGEXP) -> ContentType.JSON
        s.matches(XMLREGEXP2) -> ContentType.XML
        else -> null
      }
    }
  }
}

fun OptionalBody?.isMissing() = this == null || this.isMissing()

fun OptionalBody?.isEmpty() = this != null && this.isEmpty()

fun OptionalBody?.isNull() = this == null || this.isNull()

fun OptionalBody?.isPresent() = this != null && this.isPresent()

fun OptionalBody?.isNotPresent() = this == null || this.isNotPresent()

fun OptionalBody?.orElse(defaultValue: ByteArray) = this?.orElse(defaultValue) ?: defaultValue

fun OptionalBody?.orEmpty() = this?.orElse(ByteArray(0)) ?: ByteArray(0)

fun OptionalBody?.valueAsString() = this?.valueAsString() ?: ""

fun OptionalBody?.isNullOrEmpty() = this == null || this.isEmpty() || this.isNull()

fun OptionalBody?.unwrap() = this?.unwrap() ?: throw UnwrapMissingBodyException(
  "Failed to unwrap value from a null body")

fun OptionalBody?.orEmptyBody() = this ?: OptionalBody.empty()
