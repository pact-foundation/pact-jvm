package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import io.ktor.http.HeaderValue
import io.ktor.http.HeaderValueParam
import io.ktor.http.HeaderValueWithParameters
import io.ktor.http.parseHeaderValue
import java.util.Locale

class HeaderWithParameters(
  content: String,
  parameters: List<HeaderValueParam>
) : HeaderValueWithParameters(content, parameters)

object HeaderParser {
    private val SINGLE_VALUE_HEADERS = setOf("date", "accept-datetime", "if-modified-since", "if-unmodified-since",
      "expires", "retry-after", "last-modified", "set-cookie", "user-agent")

  fun fromJson(key: String, value: JsonValue): List<String> {
    return when {
      value is JsonValue.Array -> value.values.map { Json.toString(it).trim() }
      SINGLE_VALUE_HEADERS.contains(key.lowercase(Locale.getDefault())) -> listOf(Json.toString(value).trim())
      else -> {
        val sval = Json.toString(value).trim()
        parseHeaderValue(sval).map { hvToString(it) }
      }
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
}
