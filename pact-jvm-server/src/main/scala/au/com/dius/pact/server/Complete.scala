package au.com.dius.pact.server

import _root_.unfiltered.netty.{ReceivedMessage, ServerErrorResponse, cycle}
import _root_.unfiltered.request.HttpRequest
import _root_.unfiltered.response.ResponseFunction
import au.com.dius.pact.model._
import au.com.dius.pact.consumer._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization
import au.com.dius.pact.model.unfiltered.Conversions
import org.jboss.netty.handler.codec.http.QueryStringDecoder

object Complete {

  def getPort(j: JValue): Option[Int] = j match {
    case JObject(List(JField("port", JInt(port)))) => {
      Some(port.intValue())
    }
    case _ => None
  }

  def toJson(error: VerificationResult) = {
    implicit val formats = Serialization.formats(NoTypeHints)
    Serialization.write(error)
  }

  def apply(request: Request, oldState: ServerState): Result = {
    def clientError = Result(Response(400, None, None, None), oldState)
    def pactWritten(response: Response, port: Int) = Result(response, oldState - port)

    val result = for {
      port <- getPort(request.body)
      mockProvider <- oldState.get(port)
      sessionResults = mockProvider.session.remainingResults
      pact <- mockProvider.pact
    } yield {
      mockProvider.stop()
      
      ConsumerPactRunner.writeIfMatching(pact, sessionResults) match {
        case PactVerified => pactWritten(Response(200, Response.CrossSiteHeaders, None, null), mockProvider.config.port)
        case error => pactWritten(Response(400, Map[String, String](), toJson(error), null), mockProvider.config.port)
      }
    }
    
    result getOrElse clientError
  }

}
