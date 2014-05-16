package au.com.dius.pact.server

import _root_.unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import _root_.unfiltered.request.HttpRequest
import _root_.unfiltered.response.ResponseFunction
import au.com.dius.pact.model._
import au.com.dius.pact.consumer.{PactGenerator, MockProvider, MockProviderConfig, DefaultMockProvider}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization
import au.com.dius.pact.model.unfiltered.Conversions
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import au.com.dius.pact.consumer.VerificationResult

object ListServers {

  def apply(oldState: ServerState): Result = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val body = Serialization.write(Map("ports" -> oldState.keySet))
    Result(Response(200, Map[String, String](), body), oldState)
  }
}



case class Result(response: Response, newState: ServerState)


object Server extends App {
  val port = Integer.parseInt(args.headOption.getOrElse("29999"))

  val host: String = "localhost"
  val server = _root_.unfiltered.netty.Http.local(port).handler(RequestHandler(new ServerStateStore()))
  println(s"starting unfiltered app at 127.0.0.1 on port $port")
  server.start()
  readLine("press enter to stop server:\n")
  server.stop()
}