package au.com.dius.pact.server

import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response
import au.com.dius.pact.consumer.DefaultMockProvider
import au.com.dius.pact.consumer.StatefulMockProvider
import au.com.dius.pact.model._

import scala.collection.JavaConverters._

object RequestRouter {
  def matchPath(request: Request, oldState: ServerState): Option[StatefulMockProvider[RequestResponseInteraction]] =
    (for {
      k <- oldState.keys if (request.getPath.startsWith(k))
      pact <- oldState.get(k)
    } yield pact).headOption

  def handlePactRequest(request: Request, oldState: ServerState): Option[Response] =
    (for {
      pact <- matchPath(request, oldState)
    } yield pact.handleRequest(request)).headOption

  def state404(request: Request, oldState: ServerState): String =
    (oldState + ("path" -> request.getPath)).mkString(",\n")

  def pactDispatch(request: Request, oldState: ServerState): Response =
    // handlePactRequest(request, oldState) getOrElse new Response(404)
    handlePactRequest(request, oldState) getOrElse Response.fromMap(
      Map("status" -> 404, "body" -> state404(request, oldState)).asJava)

  def dispatch(request: Request, oldState: ServerState, config: Config): Result = {
    val urlPattern ="/(\\w*)\\?{0,1}.*".r
    val urlPattern(action) = request.getPath
    action match {
      case "create" => Create(request, oldState, config)
      case "complete" => Complete(request, oldState)
      case "" => ListServers(oldState)
      case _ => Result(pactDispatch(request, oldState), oldState)
    }
  }
}
