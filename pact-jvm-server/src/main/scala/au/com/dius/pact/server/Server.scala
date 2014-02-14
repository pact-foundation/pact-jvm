package au.com.dius.pact.server

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import akka.io
import _root_.spray.can.Http
import akka.pattern._
import _root_.spray.http.{HttpResponse, HttpRequest}
import au.com.dius.pact.model._
import au.com.dius.pact.model.spray.Conversions._
import au.com.dius.pact.consumer.{PactGeneration, PactVerification, MockServiceProvider, PactServerConfig}
import scala.concurrent.{Promise, Future}
import akka.util.Timeout
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization
import com.typesafe.config.ConfigFactory


object ListServers {

  def apply(oldState: ServerState): Future[Result] = {

    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Future.successful(Result(Response(200, Map[String,String](), body),oldState))
  }

}

object Complete {

  import PactVerification._

  implicit val executionContext = Server.actorSystem.dispatcher

  def getPort(j: JValue): Option[Int] = j match {
    case JObject(List(JField("port", JInt(port)))) => {
      Some(port.intValue())
    }
    case _ => None
  }

  def verify(pact: Pact, interactions: Iterable[Interaction]): VerificationResult = PactVerification(pact.interactions, interactions)

  def stopServer(msp: MockServiceProvider, result: Result): Future[Result] = {
    msp.stop.map {
      _ =>
        result
    }
  }

  def toJson(error: VerificationResult) = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.write(error)
  }


  def apply(request: Request, oldState: ServerState): Future[Result] = {
    val clientError = Future.successful(Result(Response(400, None, None), oldState))
    def pactWritten(response: Response, port: Int) = Result(response, oldState - port)

    val maybeMsp = getPort(request.body).flatMap(oldState.get)

    maybeMsp.map {
      msp =>
        msp.interactions.map {
          interactions =>
            val verification = verify(msp.pact, interactions)
            PactGeneration(msp.pact, verification) match {
              case PactVerified => pactWritten(Response(200, None, None), msp.config.port)
              case error => pactWritten(Response(400, Map[String, String](), toJson(error)), msp.config.port)
            }
        }.flatMap {
          r => stopServer(msp, r)
        }
    }.getOrElse(clientError)
  }

}

object Create {

  //todo: get off the spray crack
  implicit val system = Server.actorSystem
  implicit val context = Server.actorSystem.dispatcher

  def apply(request: Request, oldState: ServerState): Future[Result] = {
    val maybeJsonPact = request.body
    maybeJsonPact.map(Pact.from).map {
      pact: Pact =>
        val config = PactServerConfig()
        val server = MockServiceProvider(config, pact)
        server.start.flatMap {
          _ =>
          //todo: HACK, we should not assume only one interaction per pact
            server.enterState(pact.interactions.head.providerState).map {
              _ =>
                val entry = config.port -> server
                val body: JValue = "port" -> config.port
                Result(Response(201, Map[String, String](), body), oldState + entry)
            }
        }
    }.getOrElse(Future.successful(Result(Response(400, None, None), oldState)))
  }
}

object RequestRouter {

  def apply(request: Request, oldState: ServerState): Future[Result] = {
    request.path match {
      case "/create" => Create(request, oldState)
      case "/complete" => Complete(request, oldState)
      case "/" => ListServers(oldState)
      case _ => Future.successful(Result(Response(404, None, None), oldState))
    }
  }
}

case class Result(response: Response, newState: ServerState) {
  def sprayResponse: HttpResponse = response
}

class RequestHandler extends Actor with ActorLogging {
  implicit val executionContext = context.system.dispatcher

  def receive: Receive = handleRequests(Map())

  def handleRequests(servers: ServerState): Receive = {
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

  val started = Promise[Any]()

  implicit val timeout: Timeout = 5000L

  val port = Integer.parseInt(args.headOption.getOrElse("29999"))

  implicit val actorSystem = ActorSystem(s"Pact-Actor-System-$port", ConfigFactory.load(classOf[ActorSystem].getClassLoader), classOf[ActorSystem].getClassLoader)

  val host: String = "localhost"

  val handler = actorSystem.actorOf(Props[RequestHandler], name = s"Pact-Server:$port")

  val someFuture = io.IO(Http)(actorSystem) ? Http.Bind(handler, interface = host, port = port)

  started.completeWith(someFuture)
}