package au.com.dius.pact.server

import java.util
import au.com.dius.pact.core.model.{Request, Response, _}
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

object RequestRouter extends StrictLogging {
  def matchPath(request: Request, oldState: ServerState): Option[StatefulMockProvider] =
    (for {
      k <- oldState.getState.asScala.keys if request.getPath.startsWith(k)
      pact <- oldState.getState.asScala.get(k)
    } yield pact).headOption

  def handlePactRequest(request: Request, oldState: ServerState): Option[IResponse] =
    for {
      pact <- matchPath(request, oldState)
    } yield pact.handleRequest(request)

  def state404(request: Request, oldState: ServerState): String =
    (oldState.getState.asScala + ("path" -> request.getPath)).mkString(",\n")

  val EMPTY_MAP: util.Map[String, util.List[String]] = Map[String, util.List[String]]().asJava

  def pactDispatch(request: Request, oldState: ServerState): IResponse =
    handlePactRequest(request, oldState) getOrElse new Response(404, EMPTY_MAP,
      OptionalBody.body(state404(request, oldState).getBytes))

  def dispatch(request: Request, oldState: ServerState, config: Config): Result = {
    val urlPattern ="/(\\w*)\\?{0,1}.*".r
    val urlPattern(action) = request.getPath
    action match {
      case "create" => Create(request, oldState, config)
      case "complete" => Complete(request, oldState)
      case "publish" => Publish(request, oldState, config)
      case "" => ListServers(oldState)
      case _ => new Result(pactDispatch(request, oldState), oldState)
    }
  }
}
