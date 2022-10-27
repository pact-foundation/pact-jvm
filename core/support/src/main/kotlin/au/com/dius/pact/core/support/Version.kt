package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.parsers.StringLexer
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

data class Version(
  var major: Int,
  var minor: Int,
  var patch: Int? = null
) {
  override fun toString(): String {
    return if (patch == null) {
      "$major.$minor"
    } else {
      "$major.$minor.$patch"
    }
  }

  companion object {
    val INT = Regex("^\\d+")

    @JvmStatic
    fun parse(version: String): Result<Version, String> {
      val lexer = StringLexer(version)

      val major = when (val result = parseInt(lexer)) {
        is Ok -> result.value
        is Err -> return result
      }

      val err = parseChar('.', lexer)
      if (err != null) {
        return Err(err)
      }

      val minor = when (val result = parseInt(lexer)) {
        is Ok -> result.value
        is Err -> return result
      }

      return when {
        lexer.peekNextChar() == '.' -> {
          lexer.advance()
          when (val result = parseInt(lexer)) {
            is Ok -> if (lexer.empty) {
              Ok(Version(major, minor, result.value))
            } else {
              Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
            }
            is Err -> result
          }
        }
        lexer.empty -> Ok(Version(major, minor))
        else -> Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
      }
    }

    private fun parseChar(c: Char, lexer: StringLexer): String? {
      return when (val ch = lexer.nextChar()) {
        null -> "Was expecting a '$c' at index ${lexer.index} but got the end of the input"
        c -> null
        else -> "Was expecting a '$c' at index ${lexer.index - 1} but got '$ch'"
      }
    }

    private fun parseInt(lexer: StringLexer): Result<Int, String> {
      return when (val result = lexer.matchRegex(INT)) {
        null -> Err("Was expecting an integer at index ${lexer.index}")
        else -> Ok(result.toInt())
      }
    }
  }
}
