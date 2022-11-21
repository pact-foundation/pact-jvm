package au.com.dius.pact.core.support

import au.com.dius.pact.core.support.parsers.StringLexer

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
    @JvmStatic
    @Suppress("ReturnCount")
    fun parse(version: String): Result<Version, String> {
      val lexer = StringLexer(version)

      val major = when (val result = lexer.parseInt()) {
        is Result.Ok -> result.value
        is Result.Err -> return result
      }

      val err = parseChar('.', lexer)
      if (err != null) {
        return Result.Err(err)
      }

      val minor = when (val result = lexer.parseInt()) {
        is Result.Ok -> result.value
        is Result.Err -> return result
      }

      return when {
        lexer.peekNextChar() == '.' -> {
          lexer.advance()
          when (val result = lexer.parseInt()) {
            is Result.Ok -> if (lexer.empty) {
              Result.Ok(Version(major, minor, result.value))
            } else {
              Result.Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
            }
            is Result.Err -> result
          }
        }
        lexer.empty -> Result.Ok(Version(major, minor))
        else -> Result.Err("Unexpected characters '${lexer.remainder}' at index ${lexer.index}")
      }
    }

    private fun parseChar(c: Char, lexer: StringLexer): String? {
      return when (val ch = lexer.nextChar()) {
        null -> "Was expecting a '$c' at index ${lexer.index} but got the end of the input"
        c -> null
        else -> "Was expecting a '$c' at index ${lexer.index - 1} but got '$ch'"
      }
    }
  }
}
