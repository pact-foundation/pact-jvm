package au.com.dius.pact.provider

import au.com.dius.pact.model.unfiltered.Conversions
import au.com.dius.pact.model.{Request, Response}
import au.com.dius.pact.provider.AnimalServiceResponses.responses
import com.typesafe.scalalogging.StrictLogging
import org.json4s.JsonAST._
import org.json4s.StringInput
import org.json4s.jackson.JsonMethods.parse
import unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction

object TestService extends StrictLogging {
  var state: String = ""

  case class RequestHandler(port: Int) extends cycle.Plan
    with cycle.SynchronousExecution
    with ServerErrorResponse {
      import io.netty.handler.codec.http.{ HttpResponse=>NHttpResponse }

      def handle(request:HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
        val response = if(request.uri.endsWith("enterState")) {
          val pactRequest: Request = Conversions.unfilteredRequestToPactRequest(request)
          val json = parse(StringInput(pactRequest.body.get))
          state = (for {
            JString(s) <- json \\ "state"
          } yield s).head
          Response(200, None, None, None)
        } else {
          responses.get(state).flatMap(_.get(request.uri)).getOrElse(Response(400, None, None, None))
        }
        Conversions.pactToUnfilteredResponse(response)
      }

      def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
  }

  def apply(port:Int) = {
    val server = _root_.unfiltered.netty.Server.local(port).handler(RequestHandler(port))
    logger.info(s"starting unfiltered app at 127.0.0.1 on port $port")
    server.start()
    server
  }
}
