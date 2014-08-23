package au.com.dius.pact.provider

import AnimalServiceResponses.responses
import au.com.dius.pact.model.{Request, Response}
import org.json4s.JsonAST.{JObject, JField, JString}
import unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction
import au.com.dius.pact.model.unfiltered.Conversions
import com.typesafe.scalalogging.slf4j.StrictLogging

object TestService extends StrictLogging {
  var state: String = ""

  case class RequestHandler(port: Int) extends cycle.Plan
    with cycle.SynchronousExecution
    with ServerErrorResponse {
      import org.jboss.netty.handler.codec.http.{ HttpResponse=>NHttpResponse }

      def handle(request:HttpRequest[ReceivedMessage]): ResponseFunction[NHttpResponse] = {
        val response = if(request.uri.endsWith("enterState")) {
          val pactRequest: Request = Conversions.unfilteredRequestToPactRequest(request)
          pactRequest.body.map {
            case JObject(List(JField("state", JString(s)))) => {
              state = s
            }
          }
          Response(200, None, None, None)
        } else {
          responses.get(state).flatMap(_.get(request.uri)).getOrElse(Response(400, None, None, None))
        }
        Conversions.pactToUnfilteredResponse(response)
      }

      def intent = PartialFunction[HttpRequest[ReceivedMessage], ResponseFunction[NHttpResponse]](handle)
  }

  def apply(port:Int) = {
    val server = _root_.unfiltered.netty.Http.local(port).handler(RequestHandler(port))
    logger.info(s"starting unfiltered app at 127.0.0.1 on port $port")
    server.start()
    server
  }
}
