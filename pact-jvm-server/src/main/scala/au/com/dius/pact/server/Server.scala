package au.com.dius.pact.server

import au.com.dius.pact.model._
import au.com.dius.pact.model.finagle.Conversions._
import au.com.dius.pact.consumer.{PactGeneration, PactVerification, MockServiceProvider, PactServerConfig}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization
import com.twitter.finagle.{Service, Http}
import org.jboss.netty.handler.codec.http._
import com.twitter.util.Future

object ListServers {

  def apply(oldState: ServerState): Future[Result] = {

    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Future(Result(Response(200, Map[String,String](), body),oldState))
  }
}

object Complete {

  import PactVerification._

  def getPort(j: JValue): Option[Int] = j match {
    case JObject(List(JField("port", JInt(port)))) => {
      Some(port.intValue())
    }
    case _ => None
  }

  def verify(pact: Pact, interactions: Iterable[Interaction]): VerificationResult = PactVerification(pact.interactions, interactions)

  def toJson(error: VerificationResult) = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.write(error)
  }


  def apply(request: Request, oldState: ServerState): Future[Result] = {
    val clientError = Future(Result(Response(400, None, None), oldState))
    def pactWritten(response: Response, port: Int) = Result(response, oldState - port)

    val maybeMsp = getPort(request.body).flatMap(oldState.get)

    maybeMsp.map { msp =>
      Future {
        val verification = verify(msp.pact, msp.interactions)
        val result = PactGeneration(msp.pact, verification) match {
          case PactVerified => pactWritten(Response(200, None, None), msp.config.port)
          case error => pactWritten(Response(400, Map[String, String](), toJson(error)), msp.config.port)
        }
        msp.stop
        result
      }
    }.getOrElse(clientError)
  }

}

object Create {
  def apply(request: Request, oldState: ServerState): Future[Result] = {
    val state = new QueryStringDecoder(request.path).getParameters.get("state").get(0)
    val maybeJsonPact = request.body
    maybeJsonPact.map(Pact.from).map { pact: Pact =>
      Future {
        val config = PactServerConfig()
        val stopped = MockServiceProvider(config, pact, state)
        val server = stopped.start
        val entry = config.port -> server
        val body: JValue = "port" -> config.port
        Result(Response(201, Map[String, String](), body), oldState + entry)
      }
    }.getOrElse(Future(Result(Response(400, None, None), oldState)))
  }
}

object RequestRouter {

  def apply(request: Request, oldState: ServerState): Future[Result] = {
    request.path match {
      case "/create" => Create(request, oldState)
      case "/complete" => Complete(request, oldState)
      case "/" => ListServers(oldState)
      case _ => Future(Result(Response(404, None, None), oldState))
    }
  }
}

case class Result(response: Response, newState: ServerState) {
  def httpResponse: HttpResponse = response
}

class ServerStateStore {
  private var state: ServerState = Map()

  def getState = state
  def setState(s:ServerState) { state = s }
}

case class RequestHandler(store: ServerStateStore) extends Service[HttpRequest, HttpResponse] {
  def apply(request: HttpRequest): Future[HttpResponse] = {
    val f = RequestRouter(request, store.getState)
    f.onFailure {
      case e => println(s"MISERABLE FAILURE $e")
    }
    f.map {
      case result: Result =>
        //          log.warning(s"got result $result")
        store.setState(result.newState)
        result.httpResponse
    }
  }
}

object Server extends App {
  val port = Integer.parseInt(args.headOption.getOrElse("29999"))

  val host: String = "localhost"

  val started = Http.serve(host+":"+port, RequestHandler(new ServerStateStore()))
}