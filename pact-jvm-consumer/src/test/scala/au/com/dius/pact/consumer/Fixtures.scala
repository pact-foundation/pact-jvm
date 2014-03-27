package au.com.dius.pact.consumer

import au.com.dius.pact.model._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import au.com.dius.pact.model.finagle.Conversions._
import com.twitter.finagle.Service
import com.twitter.finagle.Http
import org.jboss.netty.handler.codec.http._

object Fixtures {
  import au.com.dius.pact.model.HttpMethod._
  import org.json4s.JsonDSL._
  val provider = Provider("test_provider")
  val consumer = Consumer("test_consumer")

  val request = Request(Get, "/",
    Map("testreqheader" -> "testreqheadervalue"),
    "test" -> true)

  val response = Response(200,
    Map("testreqheader" -> "testreqheaderval", "Access-Control-Allow-Origin" -> "*"),
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

  case class ConsumerService(serverUrl: String) {
    val client: Service[HttpRequest, HttpResponse] = Http.newService(serverUrl.replace("http://", ""))

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
      client(request.copy(path = s"$serverUrl${request.path}")).map { response: HttpResponse =>
        response.getStatus.getCode == 200 && extractFrom(response.getContent)
      }
    }
  }
}
