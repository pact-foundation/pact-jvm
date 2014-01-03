package com.dius.pact.author

import scala.concurrent.Future
import akka.actor._
import akka.pattern.ask
import akka.io
import akka.io.Tcp.Bound
import spray.http._
import spray.can.Http
import com.dius.pact.model._
import com.dius.pact.model.spray.Conversions._
import scalaz._
import Scalaz._
import akka.util.Timeout

object MockServiceProvider {
  implicit val timeout:Timeout = 1000L

  def apply(config: PactServerConfig, pact: Pact)(implicit system: ActorSystem): MockServiceProvider = {
    val ref: ActorRef = system.actorOf(Props[PactHttpServer], name="Pact-HTTP-Server")
    MockServiceProvider(config, pact, ref)
  }

  case class Start(interface: String, port: Int, pact: Pact)
  case object Started
  case object Stop
  case object Stopped
  case class EnterState(state: String)
  case object EnteredState
  case object GetInteractions
  case class CurrentInteractions(i: Seq[Interaction])
}

case class MockServiceProvider(config: PactServerConfig, pact: Pact, actorRef: ActorRef)(implicit system: ActorSystem) {
  import MockServiceProvider._

  implicit val executionContext = system.dispatcher

  def start: Future[MockServiceProvider] = {
    val f = actorRef ? Start(config.interface, config.port, pact)
    f.map(_ => this)
  }

  def stop: Future[MockServiceProvider] = {
    (actorRef ? Stop).map(_ => this)
  }

  def enterState(state:String): Future[MockServiceProvider] = {
    (actorRef ? EnterState(state)).map(_ => this)
  }

  def interactions: Future[Seq[Interaction]] = {
    (actorRef ? GetInteractions).map { case CurrentInteractions(i) => i }
  }
}

class PactRequestHandler extends Actor with ActorLogging {
  import MockServiceProvider._

  def receive = awaitPact

  def awaitPact: Receive = {
    case p: Pact => {
      log.debug(s"running pact: $p")
      context.become(awaitState(p))
    }
  }

  def awaitState(pact: Pact): Receive = awaitPact orElse {
    case EnterState(state: String) => {
      log.debug(s"entering state $state")
      sender ! EnteredState
      context.become(ready(pact, state, Seq()))
    }
  }

  def ready(pact: Pact, state: String, interactions: Seq[Interaction]): Receive = awaitState(pact) orElse {
    case Http.Connected(_, _) => {
      log.debug("client connected")
      sender ! Http.Register(self)
    }

    case request: HttpRequest => {
      log.debug(s"got request:$request")
      import RequestMatching._
      val response: Response = pact.matchRequest(request).getOrElse(Response.invalidRequest)
      sender ! pactToSprayResponse(response)
      context.become(ready(pact, state, interactions :+ Interaction("MockServiceProvider received", state, request, response)))
    }

    case GetInteractions => {
      sender ! CurrentInteractions(interactions)
    }
  }
}

class PactHttpServer extends Actor with ActorLogging {
  import MockServiceProvider._

  def receive = awaitStart

  def awaitStart: Receive = {
    case Start(interface, port, pact) => {
      val handler = context.system.actorOf(Props[PactRequestHandler], name="Pact-Request-Handler")
      io.IO(Http)(context.system) ! Http.Bind(handler, interface = interface, port = port)
      handler ! pact
      log.debug("starting")
      context.become(starting(sender, handler))
    }
  }

  def starting(client: ActorRef, requestHandler: ActorRef): Receive = {
    case Bound(_) => {
      log.debug("started")
      client ! Started
      context.become(running(sender, requestHandler))
    }
  }

  def running(stopper: ActorRef, requestHandler: ActorRef): Receive =  {
    case Stop => {
      stopper ! Http.Unbind
      context.become(stopping(sender))
    }

    case GetInteractions => {
      implicit val executionContext = context.system.dispatcher
      val client = sender
      (requestHandler ? GetInteractions).map(client !)
    }

    case EnterState(state: String) => {
      implicit val executionContext = context.system.dispatcher
      val client = sender
      (requestHandler ? EnterState(state)).map(client !)
    }
  }

  def stopping(client: ActorRef): Receive = {
    case Http.Unbound => {
      self ! PoisonPill
      client ! Stopped
    }
  }
}
