package au.com.dius.pact.server

import au.com.dius.pact.matchers.Matchers
import au.com.dius.pact.model._
import org.json4s._
import org.json4s.jackson.Serialization

object ListServers {

  def apply(oldState: ServerState): Result = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Result(Response(200, Map[String, String](), body, null), oldState)
  }
}


case class Result(response: Response, newState: ServerState)


object Server extends App {
  val port = Integer.parseInt(args.headOption.getOrElse("29999"))

  val host: String = "localhost"
  val server = _root_.unfiltered.netty.Http.local(port).handler(RequestHandler(new ServerStateStore()))
  Matchers.registerStandardMatchers()
  println(s"starting unfiltered app at 127.0.0.1 on port $port")
  server.start()
  readLine("press enter to stop server:\n")
  server.stop()
}
