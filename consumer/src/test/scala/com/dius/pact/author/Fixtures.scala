package com.dius.pact.author

import _root_.spray.can.Http
import _root_.spray.http._
import com.dius.pact.model._


import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import com.dius.pact.model.spray.Conversions

object Fixtures {
  import com.dius.pact.model.HttpMethod._
  import org.json4s.JsonDSL._
  val provider = Provider("test_provider")
  val consumer = Consumer("test_consumer")

  val request = Request(Get, "/",
    Map("testreqheader" -> "testreqheadervalue"),
    "test" -> true)

  val response = Response(200,
    Map("testreqheader" -> "testreqheaderval"),
    "responsetest" -> true)

  val interaction = Interaction(
    description = "test interaction",
    providerState = "test state",
    request = request,
    response = response
  )

  val interactions = ArrayBuffer(interaction)

  val pact: Pact = Pact(
    provider = provider,
    consumer = consumer,
    interactions = interactions
  )

  case class ConsumerService(serverUrl: String)(implicit system: ActorSystem) {
    private implicit val executionContext = system.dispatcher

    private def http(r:HttpRequest): Future[HttpResponse] = {
      ask(IO(Http), r)(5000L).mapTo[HttpResponse]
    }

    private def extractFrom(body: String): Boolean = {
      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      val result:List[Boolean] = for {
        JObject(child) <- parse(body)
        JField("responsetest", JBool(value)) <- child
      } yield value

      result.head
    }

    def hitEndpoint: Future[Boolean] = {
      http(Conversions.pactToSprayRequest(request.copy(path = s"$serverUrl${request.path}"))).map { response =>
        response.status.isSuccess && extractFrom(response.entity.asString)
      }
    }
  }
}
