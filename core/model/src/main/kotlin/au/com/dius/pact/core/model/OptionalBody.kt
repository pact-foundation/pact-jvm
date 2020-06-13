package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.ContentType.Companion.HTMLREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.JSONREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.XMLREGEXP
import au.com.dius.pact.core.model.ContentType.Companion.XMLREGEXP2
import org.apache.tika.config.TikaConfig
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata

/**
 * Class to represent missing, empty, null and present bodies
 */
data class OptionalBody(
  val state: State,
  val value: ByteArray? = null,
  var contentType: ContentType = ContentType.UNKNOWN
) {

  init {
    if (contentType == ContentType.UNKNOWN) {
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
      State.PRESENT -> "PRESENT(${value!!.toString(contentType.asCharset())})"
      State.EMPTY -> "EMPTY"
      State.NULL -> "NULL"
      State.MISSING -> "MISSING"
    }
  }

  fun valueAsString(): String {
    return when (state) {
      State.PRESENT -> value!!.toString(contentType.asCharset())
      State.EMPTY -> ""
      State.NULL -> ""
      State.MISSING -> ""
    }
  }

  fun detectContentType(): ContentType? = when {
    this.isPresent() -> {
      val metadata = Metadata()
      val mimetype = tika.detector.detect(TikaInputStream.get(value!!), metadata)
      if (mimetype.baseType.type == "text") {
        detectStandardTextContentType() ?: ContentType(mimetype)
      } else {
        ContentType(mimetype)
      }
    }
    else -> null
  }

  private fun detectStandardTextContentType(): ContentType? = when {
    isPresent() -> {
      val s = value!!.take(32).map {
        if (it == '\n'.toByte()) ' ' else it.toChar()
      }.joinToString("")
      when {
        s.matches(XMLREGEXP) -> ContentType.XML
        s.toUpperCase().matches(HTMLREGEXP) -> ContentType.HTML
        s.matches(JSONREGEXP) -> ContentType.JSON
        s.matches(XMLREGEXP2) -> ContentType.XML
        else -> null
      }
    }
    else -> null
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
    @JvmOverloads
    fun body(body: ByteArray?, contentType: ContentType = ContentType.UNKNOWN): OptionalBody {
      return when {
        body == null -> nullBody()
        body.isEmpty() -> empty()
        else -> OptionalBody(State.PRESENT, body, contentType)
      }
    }

    private val tika = TikaConfig()
  }
}

fun OptionalBody?.isMissing() = this == null || this.isMissing()

fun OptionalBody?.isEmpty() = this != null && this.isEmpty()

fun OptionalBody?.isNull() = this == null || this.isNull()

fun OptionalBody?.isPresent() = this != null && this.isPresent()

fun OptionalBody?.isNotPresent() = this == null || this.isNotPresent()

fun OptionalBody?.orElse(defaultValue: ByteArray) = this?.orElse(defaultValue) ?: defaultValue

fun OptionalBody?.orEmpty() = this?.orElse(ByteArray(0))

fun OptionalBody?.valueAsString() = this?.valueAsString() ?: ""

fun OptionalBody?.isNullOrEmpty() = this == null || this.isEmpty() || this.isNull()

fun OptionalBody?.unwrap() = this?.unwrap() ?: throw UnwrapMissingBodyException(
  "Failed to unwrap value from a null body")
