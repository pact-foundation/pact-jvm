package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.model._
import scala.concurrent.duration.FiniteDuration
import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JField, JString, JObject}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.dispatch.HttpClient

class MockServiceProviderSpec extends Specification {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  
  implicit val timeout = FiniteDuration(10L, "second")
  
  //TODO: move PactServer startup and shutdown into an around function
  "Pact Mock Service Provider" should {
    "Respond to invalid and valid requests" in {
      val config = MockProviderConfig()

      val stopped = MockServiceProvider(config, pact, interaction.providerState)

      val server = stopped.start

      val invalidRequest = request.copy(path = s"${config.url}/foo")

      val inValidResponse = HttpClient.run(invalidRequest)

      inValidResponse.map(_.status) must beEqualTo(500).await(timeout = timeout)

      //hit server with valid request
      val validRequest = request.copy(path = s"${config.url}/")
      val validResponse = HttpClient.run(validRequest)
      validResponse.map(_.status) must beEqualTo(response.status).await(timeout = timeout)

      server.stop

      val interactions = server.interactions
      interactions.size must beEqualTo(2)

      def compareRequests(actual: Request, expected: Request) = {
        actual.method must beEqualTo(expected.method)

        def trimHost(s:String) = s.replaceAll(config.url, "")
        trimHost(actual.path) must beEqualTo(trimHost(expected.path))

        val expectedHeaders = expected.headers.getOrElse(Map())
        actual.headers.map(_.filter(t => expectedHeaders.contains(t._1))) must beEqualTo(expected.headers)

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
      
      compare(interactions.head, invalidRequest, Response(500, Map("Access-Control-Allow-Origin" -> "*"),
        Some(JObject(JField("error", JString("unexpected request"))))))
      compare(interactions.tail.head, validRequest, response)
    }
  }
}