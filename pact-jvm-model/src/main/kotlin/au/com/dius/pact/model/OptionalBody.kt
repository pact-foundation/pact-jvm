package au.com.dius.pact.model

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
      return if (body == null) {
        nullBody()
      } else if (body.isEmpty()) {
        empty()
      } else {
        OptionalBody(State.PRESENT, body)
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

  fun orElse(defaultValue: String) : String? {
    return if (state == State.EMPTY || state == State.PRESENT) {
      value
    } else {
      defaultValue
    }
  }
}
