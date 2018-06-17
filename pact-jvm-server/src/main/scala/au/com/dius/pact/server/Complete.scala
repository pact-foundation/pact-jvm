package au.com.dius.pact.server

import au.com.dius.pact.consumer._
import au.com.dius.pact.core.model.{OptionalBody, Response}
import au.com.dius.pact.matchers.util.JsonUtils
import au.com.dius.pact.core.model._

import scala.collection.JavaConversions

object Complete {

  def getPort(j: Any): Option[String] = j match {
    case map: Map[AnyRef, AnyRef] => {
      if (map.contains("port")) Some(map("port").asInstanceOf[String])
      else None
    }
    case _ => None
  }

  def toJson(error: VerificationResult) = {
    OptionalBody.body("{\"error\": \"" + error + "\"}")
  }

  def apply(request: Request, oldState: ServerState): Result = {
    def clientError = Result(new Response(400), oldState)
    def pactWritten(response: Response, port: String) = Result(response, oldState - port)

    val result = for {
      port <- getPort(JsonUtils.parseJsonString(request.getBody.getValue))
      mockProvider <- oldState.get(port)
      sessionResults = mockProvider.session.remainingResults
      pact <- mockProvider.pact
    } yield {
      mockProvider.stop()

      ConsumerPactRunner.writeIfMatching(pact, sessionResults, mockProvider.config.getPactVersion) match {
        case PactVerified => pactWritten(new Response(200, JavaConversions.mapAsJavaMap(ResponseUtils.CrossSiteHeaders)),
          mockProvider.config.getPort.asInstanceOf[String])
        case error => pactWritten(new Response(400,
          JavaConversions.mapAsJavaMap(Map("Content-Type" -> "application/json")), toJson(error)),
          mockProvider.config.getPort.asInstanceOf[String])
      }
    }

    result getOrElse clientError
  }

}
