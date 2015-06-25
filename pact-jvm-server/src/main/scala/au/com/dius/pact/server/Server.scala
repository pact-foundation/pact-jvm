package au.com.dius.pact.server

import au.com.dius.pact.model._
import org.json4s._
import org.json4s.jackson.Serialization

object ListServers {

  def apply(oldState: ServerState): Result = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Result(Response(200, Map("Content-Type" -> "application/json"), body, null), oldState)
  }
}


case class Result(response: Response, newState: ServerState)


object Server extends App {
  val port = Integer.parseInt(args.headOption.getOrElse("29999"))
  val host = if (args.isDefinedAt(1)) args(1) else "localhost"
  val daemon = if (args.isDefinedAt(2)) args(2).equals("true") else false
  val server = _root_.unfiltered.netty.Server.http(port, host).handler(RequestHandler(new ServerStateStore()))
  println(s"starting unfiltered app at $host on port $port")
  server.start()
  if (!daemon) {
    readLine("press enter to stop server:\n")
    server.stop()
  }
}
