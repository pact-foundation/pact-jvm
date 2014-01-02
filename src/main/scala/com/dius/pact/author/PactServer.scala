package com.dius.pact.author

import scala.concurrent.Promise
import akka.actor._
import akka.io
import spray.can.Http
import com.dius.pact.model.{Response, Pact}
import com.dius.pact.model.spray.Conversions._
import scala.util.Try
import akka.io.Tcp.Bound
import spray.http.{HttpResponse, HttpRequest}
import RequestMatching._


case class PactServer(actorRef:ActorRef) {
  def stop = {
    actorRef ! PactServer.Stop
  }
}

object PactServer {
  implicit val system = ActorSystem()

  case class Start(interface:String, port:Int, pact:Pact)
  case object Stop

  def start(pact:Pact) = {
    val ref:ActorRef = system.actorOf(Props[PactHttpServer], name="Pact HTTP Server")
    ref ! Start(Config.interface, Config.port, pact)
  }

  class PactRequestHandler extends Actor with ActorLogging {
    val pact: Promise[Pact] = Promise()
    def receive = {
      case Http.Connected(_, _) => sender ! Http.Register(self)

      case p:Pact => pact.complete(Try(p))

      case r:HttpRequest => {
        pact.future.map { p =>
          p.matchRequest(r).getOrElse { e: Throwable =>
            HttpResponse(status = 500, entity = s"Request Invalid ${e.getMessage}")
          }
        } (Config.executionContext)
      }
    }
  }

  class PactHttpServer extends Actor with ActorLogging {
    val stopper: Promise[ActorRef] = Promise()

    def receive = {
      case Start(interface, port, pact) => {
        val handler = system.actorOf(Props[PactRequestHandler], name="Pact Request Handler")
        io.IO(Http) ! Http.Bind(handler, interface = interface, port = port)
        handler ! Pact
      }

      case Bound(_) => {
        stopper.complete(Try(sender))
      }

      case Http.Unbound => {
        system.shutdown()
      }
    }
  }
}
