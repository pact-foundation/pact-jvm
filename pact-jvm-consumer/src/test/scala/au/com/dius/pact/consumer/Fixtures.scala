package au.com.dius.pact.consumer

import java.util

import au.com.dius.pact.consumer.dispatch.HttpClient
import au.com.dius.pact.model._

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.Executors

object Fixtures {
  import au.com.dius.pact.model.HttpMethod._

  val provider = new Provider("test_provider")
  val consumer = new Consumer("test_consumer")

  val request = new Request(Post, "/", null, JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheadervalue")),
    "{\"test\": true}")

  val response = new Response(200,
    JavaConversions.mapAsJavaMap(Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*")),
    "{\"responsetest\": true}")

  val interaction = new Interaction("test interaction", "test state", request, response)

  val pact: Pact = new Pact(provider, consumer, util.Arrays.asList(interaction))

  case class ConsumerService(serverUrl: String) {
    implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

    private def extractFrom(body: String): Boolean = {
      body == "{\"responsetest\": true}"
    }

    def extractResponseTest(path: String = request.getPath): Future[Boolean] = {
      val r = request.copy
      r.setPath("$serverUrl$path")
      HttpClient.run(r).map { response =>
        response.getStatus == 200 && extractFrom(response.getBody)
      }
    }

    def simpleGet(path: String): Future[(Int, Option[String])] = {
      HttpClient.run(new Request(Get, serverUrl + path)).map { response =>
        (response.getStatus, Some(response.getBody))
      }
    }
  }
}
