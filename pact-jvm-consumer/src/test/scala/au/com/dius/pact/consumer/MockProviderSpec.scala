package au.com.dius.pact.consumer

import java.util.concurrent.Executors

import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.consumer.dispatch.HttpClient
import au.com.dius.pact.model.{RequestResponseInteraction, _}
import com.typesafe.scalalogging.StrictLogging
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.JavaConversions
import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

// Disabling as this spec does not pass on AppVeyor
//@RunWith(classOf[JUnitRunner])
class MockProviderSpec extends Specification with StrictLogging {

  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  
  implicit val timeout = FiniteDuration(5L, "second")

  def verify:ConsumerTestVerification[Result] = { r:Result =>
    if(r.isSuccess) {
      None
    } else {
      Some(r)
    }
  }

  //TODO: move PactServer startup and shutdown into an around function
  "Pact Mock Service Provider" >> {
    "Respond to invalid and valid requests" >> { implicit ee: ExecutionEnv =>
      val server = DefaultMockProvider.withDefaultConfig()

      val validRequest = request.copy()
      validRequest.setPath(server.config.url)
      val invalidRequest = request.copy()
      invalidRequest.setPath(s"${server.config.url}/foo")
      
      val Success((codeResult, results)) = server.runAndClose[Result](pact) {

        logger.debug("invalidRequest: " + invalidRequest.toString)
        val invalidResponse = FutureConverters.toScala(HttpClient.run(invalidRequest))
        logger.debug("invalidResponse: " + invalidResponse.toString)
        invalidResponse.map(_.getStatus) must be_==(500).await(3, timeout)
  
        //hit server with valid request
        val validResponse = FutureConverters.toScala(HttpClient.run(validRequest))
        logger.debug("validResponse: " + validResponse.toString)
        validResponse.map(_.getStatus) must be_==(response.getStatus).await(3, timeout)
      }

      verify(codeResult) must beNone

      results.matched.size must === (1)
      results.unexpected.size must === (1)

      def compareRequests(actual: Request, expected: Request) = {
        actual.getMethod must beEqualTo(expected.getMethod)

        def trimHost(s: String) = {
          val path = s.replaceAll(server.config.url, "")
          if (path.isEmpty) {
            "/"
          } else {
            path
          }
        }
        logger.debug("actual.getPath=" + actual.getPath)
        logger.debug("expected.getPath=" + expected.getPath)
        trimHost(actual.getPath) must beEqualTo(trimHost(expected.getPath))

        val expectedHeaders = JavaConversions.mapAsScalaMap(expected.getHeaders)
        val actualHeaders = JavaConversions.mapAsScalaMap(actual.getHeaders)
        actualHeaders.filter(t => expectedHeaders.contains(t._1)) must beEqualTo(expectedHeaders)

        actual.getBody must beEqualTo(expected.getBody)
      }

      def compare(actual: RequestResponseInteraction, request:Request, response:Response) = {
        actual.getDescription must beEqualTo(interaction.getDescription)
        actual.getProviderState must beEqualTo(interaction.getProviderState)
        compareRequests(actual.getRequest, request)

        def chunk(s:String) = s.replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", "").toLowerCase.take(10)

        chunk(actual.getResponse.getBody.orElse("")) must beEqualTo(chunk(response.getBody.orElse("")))

        val actualResponse = actual.getResponse.copy
        actualResponse.setBody(OptionalBody.empty())
        val expectedResponse = response.copy
        expectedResponse.setBody(OptionalBody.empty())
        actualResponse must beEqualTo(expectedResponse)
      }
      
      val expectedInvalidResponse = new Response(500, JavaConversions.mapAsJavaMap(Map("Access-Control-Allow-Origin" -> "*")),
        OptionalBody.body("{\"error\": \"unexpected request\"}"))

      compareRequests(results.unexpected.head, invalidRequest)
      compare(results.matched.head.asInstanceOf[RequestResponseInteraction], validRequest, response)
    }
  }
}
