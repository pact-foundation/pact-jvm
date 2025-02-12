package au.com.dius.pact.consumer

import au.com.dius.pact.core.support.isNotEmpty
import io.ktor.http.HeaderValue

object Headers {
  const val MULTIPART_HEADER_REGEX = "multipart/form-data;(\\s*charset=[^;]*;)?\\s*boundary=.*"
  val SINGLE_VALUE_HEADERS = setOf("date")
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

  fun isKnowSingleValueHeader(key: String): Boolean {
    return SINGLE_VALUE_HEADERS.contains(key.lowercase())
  }

  fun isKnowMultiValueHeader(key: String): Boolean {
    return MULTI_VALUE_HEADERS.contains(key.lowercase())
  }

  fun headerToString(headerValue: HeaderValue): String {
    return if (headerValue.params.isNotEmpty()) {
      val params = headerValue.params.joinToString(";") { "${it.name}=${it.value}" }
      "${headerValue.value};$params"
    } else {
      headerValue.value
    }
  }
}
