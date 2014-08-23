package au.com.dius.pact.consumer.specs2

import au.com.dius.pact.model._

import scala.concurrent.{ExecutionContext, Future}
import au.com.dius.pact.model.dispatch.HttpClient
import java.util.concurrent.Executors

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

  def extractResponseTest(path: String = request.path): Future[Boolean] = {
    HttpClient.run(request.copy(path = s"$serverUrl$path")).map { response =>
      response.status == 200 &&
      response.body.map(extractFrom).get
    }
  }

  def simpleGet(path: String): Future[(Int, Option[String])] = {
    HttpClient.run(Request("GET", serverUrl + path, None, None, None, None)).map { response =>
      (response.status, response.body)
    }
  }
}
