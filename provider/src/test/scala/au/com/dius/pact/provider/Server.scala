package au.com.dius.pact.provider

import akka.actor._
import scala.concurrent.{Future, ExecutionContext}
import akka.io
import spray.can.Http
import akka.io.Tcp.Bound
import akka.pattern.ask

case class Server(actor: ActorRef)(implicit system: ActorSystem) {
  import Server._
  implicit val executionContext = system.dispatcher
  //TODO: externalise timeout config
  implicit val timeout = akka.util.Timeout(10000L)

  def start(interface:String = "localhost", port:Int = 8888)(implicit executionContext:ExecutionContext): Future[Server] = {
    val f = (actor ? Start(interface, port)).map( (_) => this)
    f.onFailure { case t: Throwable => t.printStackTrace() }
    f.onSuccess { case _ => println("server started") }
    f
  }

  def stop()(implicit executionContext:ExecutionContext): Future[Server] = {
    val f = (actor ? Stop).map( (_) => this )
    f.onFailure { case t: Throwable => t.printStackTrace() }
    f.onSuccess { case _ => println("server stopped") }
    f
  }
}

object Server {
  case class Start(interface:String, port:Int)
  case object Started
  case object Stop
  case object Stopped

  def apply(implicit system: ActorSystem): Server = {
    Server(system.actorOf(Props[Main], name="test-server-main"))
  }

  class Main extends Actor with ActorLogging {
    def receive = initial

    def initial: Receive = {
      case Start(interface, port) => {
        val handler = context.system.actorOf(Props[TestService], name = "request-handler")
        io.IO(Http)(context.system) ! Http.Bind(handler, interface = interface, port = port)
        context.become(starting(sender, handler))
      }
    }

    def starting(client: ActorRef, handler: ActorRef): Receive = {
      case Bound(_) => {
        client ! Started
        context.become(started(sender, handler))
      }
    }

    def started(stopper: ActorRef, handler: ActorRef): Receive = {
      case Stop => {
        stopper ! Http.Unbind
        context.become(stopping(sender, handler))
      }
    }

    def stopping(client: ActorRef, handler: ActorRef): Receive = {
      case Http.Unbound => {
        handler ! PoisonPill
        self ! PoisonPill
        client ! Stopped
      }
    }
  }
}
