package au.com.dius.pact.consumer.specs2

import java.util.concurrent.Executors

import au.com.dius.pact.consumer.dispatch.HttpClient
import au.com.dius.pact.model.{OptionalBody, PactReader, Request}

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}

case class ConsumerService(serverUrl: String) {
  import Fixtures._
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  private def extractFrom(body: OptionalBody): Boolean = {
    body.orElse("") == "{\"responsetest\": true}"
  }

  def extractResponseTest(path: String = request.getPath): Future[Boolean] = {
    val r = request.copy()
    r.setPath(s"$serverUrl$path")
    HttpClient.run(r).map { response =>
      response.getStatus == 200 && extractFrom(response.getBody)
    }
  }

  def simpleGet(path: String): Future[(Int, String)] = {
    HttpClient.run(new Request("GET", serverUrl + path)).map { response =>
      (response.getStatus, response.getBody.getValue)
    }
  }

  def simpleGet(path: String, query: String): Future[(Int, String)] = {
    HttpClient.run(new Request("GET", serverUrl + path, PactReader.queryStringToMap(query, true))).map { response =>
      (response.getStatus, response.getBody.getValue)
    }
  }

  def options(path: String): Future[(Int, String, Map[String, String])] = {
    HttpClient.run(new Request("OPTION", serverUrl + path)).map { response =>
      (response.getStatus, response.getBody.orElse(""), JavaConversions.mapAsScalaMap(response.getHeaders).toMap)
    }
  }
}
