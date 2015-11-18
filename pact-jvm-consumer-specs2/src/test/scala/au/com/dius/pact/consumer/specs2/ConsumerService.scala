package au.com.dius.pact.consumer.specs2

import java.util.concurrent.Executors

import au.com.dius.pact.model.Request

import scala.collection.JavaConversions
import scala.concurrent.{ExecutionContext, Future}

case class ConsumerService(serverUrl: String) {
  import Fixtures._
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  private def extractFrom(body: String): Boolean = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._

    val result:List[Boolean] = for {
      JObject(child) <- parse(body)
      JField("responsetest", JBool(value)) <- child
    } yield value

    result.head
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
      (response.getStatus, response.getBody)
    }
  }

  def options(path: String): Future[(Int, String, Map[String, String])] = {
    HttpClient.run(new Request("OPTION", serverUrl + path)).map { response =>
      (response.getStatus, response.getBody, JavaConversions.mapAsScalaMap(response.getHeaders).toMap)
    }
  }
}
