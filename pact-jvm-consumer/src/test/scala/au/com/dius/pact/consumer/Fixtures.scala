package au.com.dius.pact.consumer

import java.util
import java.util.concurrent.Executors

import au.com.dius.pact.consumer.dispatch.HttpClient
import au.com.dius.pact.model._

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}

object Fixtures {

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val headers = Map("testreqheader" -> "testreqheadervalue", "Content-Type" -> "application/json")
  val request = new Request("POST", "/", null, JavaConversions.mapAsJavaMap(headers), OptionalBody.body("{\"test\": true}"))

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*")),
    OptionalBody.body("{\"responsetest\": true}"))

  val interaction = new RequestResponseInteraction("test interaction", "test state", request, response)

  val pact: RequestResponsePact = new RequestResponsePact(provider, consumer, util.Arrays.asList(interaction))

  case class ConsumerService(serverUrl: String) {
    implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

    private def extractFrom(body: OptionalBody): Boolean = {
      body.orElse("") == "{\"responsetest\": true}"
    }

    def extractResponseTest(path: String = request.getPath): Future[Boolean] = {
      val r = request.copy
      r.setPath("$serverUrl$path")
      HttpClient.run(r).map { response =>
        response.getStatus == 200 && extractFrom(response.getBody)
      }
    }

    def simpleGet(path: String): Future[(Int, Option[String])] = {
      HttpClient.run(new Request("GET", serverUrl + path)).map { response =>
        (response.getStatus, Some(response.getBody.orElse("")))
      }
    }
  }
}
