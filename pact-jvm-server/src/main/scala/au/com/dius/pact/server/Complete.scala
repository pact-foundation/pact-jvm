package au.com.dius.pact.server

import java.io.File

import au.com.dius.pact.core.model._

import scala.collection.JavaConverters._
import scala.util.Success

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
    def pactWritten(response: Response, port: String) = {
      val server = oldState(port)
      val newState = oldState.filter(p => p._2 != server)
      Result(response, newState)
    }

    val result = for {
      port <- getPort(JsonUtils.parseJsonString(request.getBody.valueAsString()))
      mockProvider <- oldState.get(port)
      sessionResults = mockProvider.session.remainingResults
      pact <- mockProvider.pact
    } yield {
      mockProvider.stop()

      writeIfMatching(pact, sessionResults, mockProvider.config.getPactVersion) match {
        case PactVerified => pactWritten(new Response(200, ResponseUtils.CrossSiteHeaders.asJava),
          mockProvider.config.getPort.toString)
        case error => pactWritten(new Response(400,
          Map("Content-Type" -> List("application/json").asJava).asJava, toJson(error)),
          mockProvider.config.getPort.toString)
      }
    }

    result getOrElse clientError
  }

  def writeIfMatching(pact: Pact[RequestResponseInteraction], results: PactSessionResults, pactVersion: PactSpecVersion) = {
    if (results.allMatched) {
      val pactFile = destinationFileForPact(pact)
      DefaultPactWriter.INSTANCE.writePact(pactFile, pact, pactVersion)
    }
    VerificationResult(Success(results))
  }

  def defaultFilename[I <: Interaction](pact: Pact[RequestResponseInteraction]): String = s"${pact.getConsumer.getName}-${pact.getProvider.getName}.json"

  def destinationFileForPact[I <: Interaction](pact: Pact[RequestResponseInteraction]): File = destinationFile(defaultFilename(pact))

  def destinationFile(filename: String) = new File(s"${System.getProperty("pact.rootDir", "target/pacts")}/$filename")

}
