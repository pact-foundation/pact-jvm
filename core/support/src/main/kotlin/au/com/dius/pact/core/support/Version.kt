package au.com.dius.pact.core.support

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
      var buffer = version
      var index = 0

      val major = when (val result = parseInt(buffer, index)) {
        is Ok -> {
          buffer = result.value.second
          index = result.value.third
          result.value.first
        }
        is Err -> return result
      }

      when (val dot = parseChar('.', buffer, index)) {
        is Ok -> {
          buffer = dot.value.first
          index = dot.value.second
        }
        is Err -> {
          return dot
        }
      }

      val minor = when (val result = parseInt(buffer, index)) {
        is Ok -> {
          buffer = result.value.second
          index = result.value.third
          result.value.first
        }
        is Err -> return result
      }

      val dot = parseChar('.', buffer, index)
      return when {
        dot is Ok -> {
          buffer = dot.value.first
          index = dot.value.second
          when (val result = parseInt(buffer, index)) {
            is Ok -> Ok(Version(major, minor, result.value.first))
            is Err -> result
          }
        }
        buffer.isEmpty() -> Ok(Version(major, minor))
        else -> Err("Unexpected character '${buffer[0]}' at index $index")
      }
    }

    private fun parseChar(c: Char, buffer: String, index: Int): Result<Pair<String, Int>, String> {
      return when {
        buffer.isNotEmpty() && buffer[0] == c -> {
          Ok(buffer.substring(1) to (index + 1))
        }
        else -> Err("Was expecting a $c at index $index")
      }
    }

    private fun parseInt(buffer: String, index: Int): Result<Triple<Int, String, Int>, String> {
      return when (val result = INT.find(buffer)) {
        null -> Err("Was expecting an integer at index $index")
        else -> {
          val i = result.value.toInt()
          Ok(Triple(i, buffer.substring(result.value.length), index + result.value.length))
        }
      }
    }
  }
}
