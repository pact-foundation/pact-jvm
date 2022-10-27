package au.com.dius.pact.core.support.parsers

class StringLexer(private val buffer: String) {
  var index = 0
    private set

  val empty: Boolean
    get() = index >= buffer.length

  val remainder: String
    get() = buffer.substring(index)

  fun nextChar(): Char? {
    val c = peekNextChar()
    if (c != null) {
      index++
    }
    return c
  }

  fun peekNextChar(): Char? {
    return if (empty) {
      null
    } else {
      buffer[index]
    }
  }

  fun advance() {
    advance(1)
  }

  fun advance(count: Int) {
    for (i in 0 until count) {
      index++
    }
  }

  fun skipWhitespace() {
    var next = peekNextChar()
    while (next != null && Character.isWhitespace(next)) {
      advance()
      next = peekNextChar()
    }
  }

  fun matchRegex(regex: Regex): String? {
    return when (val result = regex.find(buffer.substring(index))) {
      null -> null
      else -> {
        index += result.value.length
        result.value
      }
    }
  }
}
