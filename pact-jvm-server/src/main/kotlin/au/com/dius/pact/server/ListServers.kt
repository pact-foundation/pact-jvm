package au.com.dius.pact.server

import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Response

object ListServers {
  private val portRegex = Regex("\\d+")

  @JvmStatic
  fun apply(oldState: ServerState): Result {
    val ports = oldState.state.keys.filter { it.matches(portRegex) }.joinToString(", ")
    val paths = oldState.state.keys.filter { !it.matches(portRegex) }.joinToString(", ") { "\"$it\"" }
    val body = OptionalBody.body(("{\"ports\": [$ports], \"paths\": [$paths]}").toByteArray())
    return Result(Response(200, mapOf("Content-Type" to listOf("application/json")).toMutableMap(), body), oldState)
  }
}
