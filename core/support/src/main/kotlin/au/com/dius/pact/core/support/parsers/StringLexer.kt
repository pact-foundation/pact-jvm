package au.com.dius.pact.core.support.parsers

import au.com.dius.pact.core.support.Result

open class StringLexer(val buffer: String) {
  var index = 0
    private set

  val empty: Boolean
    get() = index >= buffer.length

  val remainder: String
    get() = buffer.substring(index)

  var lastMatch: String? = null
    private set

  fun nextChar(): Char? {
    val c = peekNextChar()
    if (c != null) {
      lastMatch = c.toString()
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
    for (_i in 0 until count) {
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
        lastMatch = result.value
        result.value
      }
    }
  }

  fun matchString(s: String): Boolean {
    return if (buffer.startsWith(s, index)) {
      index += s.length
      lastMatch = s
      true
    } else {
      false
    }
  }

  fun matchChar(c: Char): Boolean {
    return if (peekNextChar() == c) {
      index++
      lastMatch = c.toString()
      true
    } else {
      false
    }
  }

  fun parseInt(): Result<Int, String> {
    return when (val result = matchRegex(INT)) {
      null -> Result.Err("Was expecting an integer at index $index")
      else -> {
        lastMatch = result
        Result.Ok(result.toInt())
      }
    }
  }

  companion object {
    val INT = Regex("^\\d+")
  }
}
