package au.com.dius.pact.core.model

/**
 * Class to represent missing, empty, null and present bodies
 */
data class OptionalBody(val state: State, val value: String? = null) {

  enum class State {
    MISSING, EMPTY, NULL, PRESENT
  }

  companion object {

    @JvmStatic fun missing(): OptionalBody {
      return OptionalBody(State.MISSING)
    }

    @JvmStatic fun empty(): OptionalBody {
      return OptionalBody(State.EMPTY, "")
    }

    @JvmStatic fun nullBody(): OptionalBody {
      return OptionalBody(State.NULL)
    }

    @JvmStatic fun body(body: String?): OptionalBody {
      return when {
        body == null -> nullBody()
        body.isEmpty() -> empty()
        else -> OptionalBody(State.PRESENT, body)
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

  fun orElse(defaultValue: String): String {
    return if (state == State.EMPTY || state == State.PRESENT) {
      this.value!!
    } else {
      defaultValue
    }
  }

  fun unwrap(): String {
    if (isPresent()) {
      return value!!
    } else {
      throw UnwrapMissingBodyException("Failed to unwrap value from a $state body")
    }
  }
}

fun OptionalBody?.isMissing() = this == null || this.isMissing()

fun OptionalBody?.isEmpty() = this != null && this.isEmpty()

fun OptionalBody?.isNull() = this == null || this.isNull()

fun OptionalBody?.isPresent() = this != null && this.isPresent()

fun OptionalBody?.isNotPresent() = this == null || this.isNotPresent()

fun OptionalBody?.orElse(defaultValue: String) = this?.orElse(defaultValue) ?: defaultValue
