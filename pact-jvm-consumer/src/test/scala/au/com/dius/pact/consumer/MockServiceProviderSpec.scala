package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.model._
import au.com.dius.pact.model.finagle.Conversions._
import scala.concurrent.duration.FiniteDuration
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JField, JString, JObject}
import com.twitter.finagle.Http
import org.jboss.netty.handler.codec.http._
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.Executors
import au.com.dius.pact.model.Interaction

class MockServiceProviderSpec extends Specification {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
  
  implicit val timeout = FiniteDuration(10L, "second")
  
  //TODO: move PactServer startup and shutdown into an around function
  "Pact Server" should {
    "Work" in {
      val config = PactServerConfig()

      val stopped = MockServiceProvider(config, pact, interaction.providerState)

      val server = stopped.start

      val http = Http.newService(config.interface + ":" + config.port)

      val host = s"${config.interface}:${config.port}"
      //hit server with invalid request, add spray headers for easier verification
      val invalidRequest = request.copy(path = s"${config.url}/foo")

      val inValidResponse: Future[HttpResponse] = http(invalidRequest)

      inValidResponse.map{_.status.intValue} must beEqualTo(500).await(timeout = timeout)

      //hit server with valid request
      val validRequest = request.copy(path = s"${config.url}/")
      val validResponse: Future[HttpResponse] = http(validRequest)
      validResponse.map{_.status.intValue} must beEqualTo(response.status).await(timeout = timeout)

      server.stop

      val interactions = server.interactions
      interactions.size must beEqualTo(2)

      def compareRequests(actual: Request, expected: Request) = {
        actual.method must beEqualTo(expected.method)
        def trimHost(s:String) = s.replaceAll(config.url, "")
        trimHost(actual.path) must beEqualTo(trimHost(expected.path))
        def addHeaders(m:Map[String, String]):Map[String, String] = {
          val hostHeader = "Host" -> (config.interface+":"+config.port)
          val contentLengthHeader = "Content-Length" -> "13"
          m + hostHeader + contentLengthHeader
        }
        actual.headers.map(addHeaders) must beEqualTo(expected.headers.map(addHeaders))
        actual.bodyString must beEqualTo(expected.bodyString)
      }

      def compare(actual: Interaction, request:Request, response:Response) = {
        actual.description must beEqualTo("MockServiceProvider received")
        actual.providerState must beEqualTo(interaction.providerState)
        compareRequests(actual.request, request)

        def chunk(s:String) = s.replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", "").toLowerCase.take(10)

        actual.response.bodyString.map(chunk) must beEqualTo(response.bodyString.map(chunk))

        actual.response.copy(body = None) must beEqualTo(response.copy(body = None))
      }
      
      compare(interactions.head, invalidRequest, Response(500, None, Some(JObject(JField("error", JString("unexpected request"))))))
      compare(interactions.tail.head, validRequest, response)
    }
  }
}