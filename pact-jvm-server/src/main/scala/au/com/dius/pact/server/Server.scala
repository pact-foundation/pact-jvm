package au.com.dius.pact.server

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import akka.io
import _root_.spray.can.Http
import akka.pattern._
import _root_.spray.http.{HttpResponse, HttpRequest}
import au.com.dius.pact.model._
import au.com.dius.pact.model.spray.Conversions._
import au.com.dius.pact.consumer.{MockServiceProvider, PactServerConfig}
import scala.concurrent.Future
import akka.util.Timeout
import org.json4s._
import org.json4s.JsonDSL._

object Create {

  //todo: get off the spray crack
  implicit val system = Server.actorSystem
  implicit val context = Server.actorSystem.dispatcher

  def apply(request: Request, oldState: Map[Int, MockServiceProvider]): Future[Result] = {
    val maybeJsonPact = request.body
    maybeJsonPact.map(Pact.from).map {
      pact: Pact =>
        val config = PactServerConfig()
        val server = MockServiceProvider(config, pact)
        server.start.flatMap {
          _ =>
            //todo: HACK, we should not assume only one interaction per pact
            server.enterState(pact.interactions.head.providerState).map { _ =>
              val entry = config.port -> server
              val body:JValue = "port" -> config.port
              Result(Response(201, Map[String, String](), body), oldState + entry)
            }
        }
    }.getOrElse(Future.successful(Result(Response(400, None, None), oldState)))
  }
}

object RequestRouter {

  def apply(request: Request, oldState: Map[Int, MockServiceProvider]): Future[Result] = {
    request.path match {
      case "/create" => Create(request, oldState)
      case _ => Future.successful(Result(Response(404, None, None), oldState))
    }
  }
}

case class Result(response: Response, newState: Map[Int, MockServiceProvider]) {
  def sprayResponse: HttpResponse = response
}

class RequestHandler extends Actor with  ActorLogging {
  implicit val executionContext = context.system.dispatcher

  def receive: Receive = handleRequests(Map())

  def handleRequests(servers: Map[Int, MockServiceProvider]): Receive = {
    case Http.Connected(_, _) => {
      sender ! Http.Register(self)
    }

    case request: HttpRequest => {
      val f = RequestRouter(request, servers)
      val client = sender
      f.onSuccess {
        case result: Result =>
//          log.warning(s"got result $result")
          client ! result.sprayResponse
          context.become(handleRequests(result.newState))
      }

      f.onFailure {
        case e => log.error(s"MISERABLE FAILURE $e")
      }
    }
  }
}

object Server extends App {
  implicit val timeout:Timeout = 5000L

  val port = Integer.parseInt(args.headOption.getOrElse("29999"))

  implicit val actorSystem = ActorSystem("Pact-Actor-System")

  val host: String = "localhost"

  val handler = actorSystem.actorOf(Props[RequestHandler], name=s"Pact-Server:$port")

  val someFuture = io.IO(Http)(actorSystem) ? Http.Bind(handler, interface = host, port = port)

  //TODO: shut down server gracefully at end of process

}