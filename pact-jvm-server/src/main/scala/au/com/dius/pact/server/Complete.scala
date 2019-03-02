package au.com.dius.pact.server

import au.com.dius.pact.consumer._
import au.com.dius.pact.matchers.util.JsonUtils
import au.com.dius.pact.model._

import scala.collection.JavaConverters._

object Complete {

  def getPort(j: Any): Option[String] = j match {
    case map: Map[AnyRef, AnyRef] => {
      if (map.contains("port")) Some(map("port").toString)
      else None
    }
    case _ => None
  }

  def toJson(error: VerificationResult) = {
    OptionalBody.body(("{\"error\": \"" + error + "\"}").getBytes)
  }

  def apply(request: Request, oldState: ServerState): Result = {
    def clientError = Result(new Response(400), oldState)
    def pactWritten(response: Response, port: String) = Result(response, oldState - port)

    val result = for {
      port <- getPort(JsonUtils.parseJsonString(request.getBody.valueAsString()))
      mockProvider <- oldState.get(port)
      sessionResults = mockProvider.session.remainingResults
      pact <- mockProvider.pact
    } yield {
      mockProvider.stop()

      ConsumerPactRunner.writeIfMatching(pact, sessionResults, mockProvider.config.getPactVersion) match {
        case PactVerified => pactWritten(new Response(200, ResponseUtils.CrossSiteHeaders.asJava),
          mockProvider.config.getPort.toString)
        case error => pactWritten(new Response(400,
          Map("Content-Type" -> List("application/json").asJava).asJava, toJson(error)),
          mockProvider.config.getPort.toString)
      }
    }

    result getOrElse clientError
  }

}
