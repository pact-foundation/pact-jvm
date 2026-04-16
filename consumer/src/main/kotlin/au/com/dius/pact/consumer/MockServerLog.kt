package au.com.dius.pact.consumer

import au.com.dius.pact.core.model.IRequest
import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.support.json.JsonParser

/**
 * Formats HTTP requests and responses for mock server debug logging.
 *
 * Requests are prefixed with `>>>` and responses with `<<<` so they are easy
 * to distinguish at a glance in a log stream.
 */
object MockServerLog {

  fun requestToString(request: IRequest): String = buildString {
    appendLine(">>> ${request.method} ${request.path}${queryString(request)}")
    request.headers.forEach { (key, values) ->
      appendLine("  $key: ${values.joinToString(", ")}")
    }
    bodyBlock(request.body)?.let { append(it) }
  }.trimEnd()

  fun responseToString(response: IResponse): String = buildString {
    appendLine("<<< ${response.status} ${httpStatusDescription(response.status)}")
    response.headers.forEach { (key, values) ->
      appendLine("  $key: ${values.joinToString(", ")}")
    }
    bodyBlock(response.body)?.let { append(it) }
  }.trimEnd()

  private fun queryString(request: IRequest): String {
    if (request.query.isEmpty()) return ""
    return "?" + request.query.entries
      .flatMap { (k, vs) -> vs.map { "$k=$it" } }
      .joinToString("&")
  }

  @Suppress("TooGenericExceptionCaught")
  private fun bodyBlock(body: OptionalBody): String? = when {
    body.isMissing() -> null
    body.isEmpty() -> "\n  (empty body)"
    body.isNull() -> "\n  (null body)"
    body.contentType.isJson() -> "\n" + runCatching {
      JsonParser.parseString(body.valueAsString()).prettyPrint(2)
    }.getOrElse { "  ${body.valueAsString()}" }
    body.contentType.isText() || body.contentType.isXml() ->
      "\n" + body.valueAsString().lines().joinToString("\n") { "  $it" }
    else -> "\n  (${body.unwrap().size} bytes of binary data)"
  }

  private fun httpStatusDescription(status: Int): String =
    HTTP_STATUS_DESCRIPTIONS[status] ?: ""

  private val HTTP_STATUS_DESCRIPTIONS = mapOf(
    100 to "Continue",
    101 to "Switching Protocols",
    200 to "OK",
    201 to "Created",
    202 to "Accepted",
    204 to "No Content",
    206 to "Partial Content",
    301 to "Moved Permanently",
    302 to "Found",
    304 to "Not Modified",
    307 to "Temporary Redirect",
    308 to "Permanent Redirect",
    400 to "Bad Request",
    401 to "Unauthorized",
    403 to "Forbidden",
    404 to "Not Found",
    405 to "Method Not Allowed",
    406 to "Not Acceptable",
    408 to "Request Timeout",
    409 to "Conflict",
    410 to "Gone",
    415 to "Unsupported Media Type",
    422 to "Unprocessable Entity",
    429 to "Too Many Requests",
    500 to "Internal Server Error",
    501 to "Not Implemented",
    502 to "Bad Gateway",
    503 to "Service Unavailable",
    504 to "Gateway Timeout"
  )
}
