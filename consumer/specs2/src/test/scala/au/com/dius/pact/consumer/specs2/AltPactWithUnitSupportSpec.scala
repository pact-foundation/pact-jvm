package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit._

import au.com.dius.pact.core.model.{Request, RequestResponsePact, Response}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

@RunWith(classOf[JUnitRunner])
class AltPactWithUnitSupportSpec extends Specification with PactSpec with UnitSpecsSupport {
  sequential

  override val provider: String = "AltSpecsProvider"
  override val consumer: String = "AltSpecsConsumer"

  val timeout: FiniteDuration = Duration(5000, MILLISECONDS)

  val fooRequest: Request = buildRequest(path = "/foo")
  val fooResponse: Response = buildResponse(maybeBody = Some("{}"))
  val optionRequest: Request = buildRequest(path = "/", method = "OPTION")
  val optionResponse: Response = buildResponse(headers = Map("Option" -> "Value-X"))

  override val pactFragment: RequestResponsePact = buildPactFragment(
    consumer = consumer,
    provider = provider,
    interactions = List(
      buildInteraction("a request for foo", List(), fooRequest, fooResponse),
      buildInteraction("an option request", List(), optionRequest, optionResponse)
    )
  )

  description(pactFragment) >> {
    "GET returns a 200 status and empty body" >> {
      val simpleGet = ConsumerService(mockHttpServer.getUrl).simpleGet("/foo")
      Await.result(simpleGet, timeout) must be_==(200, "{}")
    }

    "OPTION returns a 200 status and the correct headers" >> {
      val optionsResult = ConsumerService(mockHttpServer.getUrl).options("/")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Option" -> "Value-X"))
    }
  }
}
