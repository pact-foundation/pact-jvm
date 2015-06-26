package au.com.dius.pact.server

import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response


object RequestRouter {
  def dispatch(request: Request, oldState: ServerState, config: Config): Result = {
    val urlPattern ="/(\\w*)\\?{0,1}.*".r
    val urlPattern(action) = request.path
    action match {
      case "create" => Create(request, oldState, config)
      case "complete" => Complete(request, oldState)
      case "" => ListServers(oldState)
      case _ => Result(Response(404, None, None, None), oldState)
    }
  }
}
