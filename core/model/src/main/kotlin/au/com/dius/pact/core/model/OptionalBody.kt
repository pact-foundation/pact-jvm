package au.com.dius.pact.core.model

import java.nio.charset.Charset

/**
 * Class to represent missing, empty, null and present bodies
 */
data class OptionalBody(
  val state: State,
  val value: ByteArray? = null,
  val contentType: ContentType = ContentType.UNKNOWN
) {

  enum class State {
    MISSING, EMPTY, NULL, PRESENT
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
      State.PRESENT -> "PRESENT(${value!!.toString(Charset.defaultCharset())})"
      State.EMPTY -> "EMPTY"
      State.NULL -> "NULL"
      State.MISSING -> "MISSING"
    }
  }

  fun valueAsString(): String {
    return when (state) {
      State.PRESENT -> value!!.toString(Charset.defaultCharset())
      State.EMPTY -> ""
      State.NULL -> ""
      State.MISSING -> ""
    }
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

fun OptionalBody?.unwrap() = this?.unwrap() ?: throw throw UnwrapMissingBodyException("Failed to unwrap value from a null body")
