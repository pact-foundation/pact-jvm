package com.dius.pact.runner.http

import akka.actor._
import scala.concurrent.{ExecutionContext, Promise}
import akka.io
import spray.can.Http
import akka.io.Tcp.Bound
import scala.util.Try

object Server {
  implicit val system = ActorSystem()

  val actor = Promise[ActorRef]

  class Main extends Actor with ActorLogging {
    val stopper:Promise[ActorRef] = Promise()

    def receive = {
      case "start" => {
        val handler = system.actorOf(Props[Service], name = "handler")

        io.IO(Http) ! Http.Bind(handler, interface = "localhost", port = 8888)
      }

      case "stop" => {
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

  def start() {
    actor.complete(Try(system.actorOf(Props[Main], name="main")))
  }

  def stop() = {
    //TODO: find out what different execution contexts mean
    import ExecutionContext.Implicits.global
    actor.future.map(_ ! "stop")
  }

}
