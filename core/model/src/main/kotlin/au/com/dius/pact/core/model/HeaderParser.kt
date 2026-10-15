package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import io.ktor.http.HeaderValue
import io.ktor.http.HeaderValueParam
import io.ktor.http.HeaderValueWithParameters
import io.ktor.http.parseHeaderValue
import java.util.Locale

val SINGLE_VALUE_HEADERS = setOf("date", "accept-datetime", "if-modified-since", "if-unmodified-since",
  "expires", "retry-after", "last-modified", "set-cookie", "user-agent")
val PARAMETERISED_HEADERS = setOf("accept", "content-type")

/** Standard HTTP headers whose values are comma-separated lists (RFC 7230/9110). Comma whitespace
 *  normalisation is only applied to these headers to avoid corrupting custom headers (e.g. JSON). */
val MULTI_VALUE_HEADERS = setOf(
  "accept",
  "accept-encoding",
  "accept-language",
  "access-control-allow-headers",
  "access-control-allow-methods",
  "access-control-expose-headers",
  "access-control-request-headers",
  "allow",
  "cache-control",
  "if-match",
  "if-none-match",
  "vary"
)

class HeaderWithParameters(
  content: String,
  parameters: List<HeaderValueParam>
) : HeaderValueWithParameters(content, parameters)

object HeaderParser {
  fun fromJson(key: String, value: JsonValue): List<String> {
    val name = key.lowercase(Locale.getDefault())
    return when {
      value is JsonValue.Array -> value.values.map { Json.toString(it).trim() }
      SINGLE_VALUE_HEADERS.contains(name) -> listOf(Json.toString(value).trim())
      MULTI_VALUE_HEADERS.contains(name) || PARAMETERISED_HEADERS.contains(name) -> {
        val sval = Json.toString(value).trim()
        parseHeaderValue(sval).map { hvToString(it) }
      }
      // Any other header is kept as a single opaque value. Splitting it on commas corrupts custom
      // headers (bearer tokens, JSON, etc.) when an existing pact file is read back and merged,
      // because the parts get re-joined with ", " on write. See #1893.
      else -> listOf(Json.toString(value).trim())
    }
  }

  fun hvToString(headerValue: HeaderValue): String {
    return if (headerValue.params.isEmpty()) {
      headerValue.value.trim()
    } else {
      val h = HeaderWithParameters(headerValue.value, headerValue.params)
      h.toString()
    }
  }

  /**
   * Splits the header value into the value and the attributes as a Map
   */
  fun headerValueToMap(value: String): Pair<String, Map<String, String>> {
    val header = parseHeaderValue(value)
    return header[0].value to header[0].params.fold(mutableMapOf()) {
      acc, value -> acc[value.name] = value.value; acc
    }
  }
}
