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
import scala.util.{Try, Success, Failure}

class MockProviderSpec extends Specification {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  
  implicit val timeout = FiniteDuration(10L, "second")
  
  //TODO: move PactServer startup and shutdown into an around function
  "Pact Mock Service Provider" should {
    "Respond to invalid and valid requests" in {
      val server = DefaultMockProvider.withDefaultConfig()

      val validRequest = request.copy(path = s"${server.config.url}/")
      val invalidRequest = request.copy(path = s"${server.config.url}/foo")
      
      val Success(results) = server.runAndClose(pact) {
  
        val invalidResponse = HttpClient.run(invalidRequest)
        invalidResponse.map(_.status) must beEqualTo(500).await(timeout = timeout)
  
        //hit server with valid request
        val validResponse = HttpClient.run(validRequest)
        validResponse.map(_.status) must beEqualTo(response.status).await(timeout = timeout)

      }

      results.matched.size must === (1)
      results.unexpected.size must === (1)
      
      

      def compareRequests(actual: Request, expected: Request) = {
        actual.method must beEqualTo(expected.method)

        def trimHost(s: String) = s.replaceAll(server.config.url, "")
        trimHost(actual.path) must beEqualTo(trimHost(expected.path))

        val expectedHeaders = expected.headers.getOrElse(Map())
        actual.headers.map(_.filter(t => expectedHeaders.contains(t._1))) must beEqualTo(expected.headers)

        actual.bodyString must beEqualTo(expected.bodyString)
      }

      def compare(actual: Interaction, request:Request, response:Response) = {
        actual.description must beEqualTo(interaction.description)
        actual.providerState must beEqualTo(interaction.providerState)
        compareRequests(actual.request, request)

        def chunk(s:String) = s.replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", "").toLowerCase.take(10)

        actual.response.bodyString.map(chunk) must beEqualTo(response.bodyString.map(chunk))

        actual.response.copy(body = None) must beEqualTo(response.copy(body = None))
      }
      
      val expectedInvalidResponse = Response(500, Map("Access-Control-Allow-Origin" -> "*"),
        Some(JObject(JField("error", JString("unexpected request")))))
      
      compareRequests(results.unexpected.head, invalidRequest)
      compare(results.matched.head, validRequest, response)
    }
  }
}