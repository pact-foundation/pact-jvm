package au.com.dius.pact.server

import au.com.dius.pact.core.model.IResponse
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.Response
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

object RequestRouter {
  fun matchPath(request: Request, oldState: ServerState): StatefulMockProvider? {
    return oldState.state.entries.firstOrNull { request.path.startsWith(it.key) }?.value
  }

  fun handlePactRequest(request: Request, oldState: ServerState): IResponse? {
    val pact = matchPath(request, oldState)
    return pact?.handleRequest(request)
  }

  fun state404(request: Request, oldState: ServerState) =
    (oldState.state.entries.map { it.toPair() } + ("path" to request.path)).joinToString(",\n") { "${it.first} -> ${it.second}" }

  fun pactDispatch(request: Request, oldState: ServerState) =
    handlePactRequest(request, oldState) ?: Response(404, mutableMapOf(),
      OptionalBody.body(state404(request, oldState).toByteArray()))

  private val urlPattern = Regex("/(\\w*)\\?{0,1}.*")

  @JvmStatic
  fun dispatch(request: Request, oldState: ServerState, config: Config): Result {
    val matchResult = urlPattern.find(request.path)
    val (action) = matchResult!!.destructured
    return when (action) {
      "create" -> Create.apply(request, oldState, config)
      "complete" -> Complete.apply(request, oldState)
      "publish" -> Publish.apply(request, oldState, config)
      "" -> ListServers.apply(oldState)
      else -> Result(pactDispatch(request, oldState), oldState)
    }
  }
}
