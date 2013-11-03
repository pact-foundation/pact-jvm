package com.dius.pact.runner

import akka.actor._
import scala.concurrent.{ExecutionContext, Promise}
import akka.io
import spray.can.Http
import akka.io.Tcp.Bound
import scala.util.Try

object Server {
  case class Start(interface:String, port:Int)
  case object Stop

  implicit val system = ActorSystem()

  val actor = Promise[ActorRef]()

  def start(interface:String = "localhost", port:Int = 8888)(implicit executionContext:ExecutionContext) {
    actor.complete(Try(system.actorOf(Props[Main], name="main")))
    actor.future.map(_ ! Start(interface, port))
  }

  def stop()(implicit executionContext:ExecutionContext) = {
    actor.future.map(_ ! Stop)
  }

  class Main extends Actor with ActorLogging {
    val stopper:Promise[ActorRef] = Promise()

    def receive = {
      case Start(interface, port) => {
        val handler = system.actorOf(Props[TestService], name = "handler")

        io.IO(Http) ! Http.Bind(handler, interface = interface, port = port)
      }

      case Stop => {
        import system.dispatcher

        stopper.future.map( _ ! Http.Unbind)
      }

      case Http.Unbound => {
        log.info("server stopped, shutting down actor system")
        system.shutdown()
      }

      case Bound(_) => {
        stopper.complete(Try(sender))
        log.info("server bound")
      }

      case m => {
        log.info(s"unrecognised message $m")
      }
    }
  }
}
