package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit._

import au.com.dius.pact.consumer._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class AltPactWithUnitSupportSpec extends Specification with PactSpec with UnitSpecsSupport {
  sequential

  override val provider: String = "AltSpecsProvider"
  override val consumer: String = "AltSpecsConsumer"

  val timeout = Duration(5000, MILLISECONDS)

  val fooRequest = buildRequest(path = "/foo")
  val fooResponse = buildResponse(maybeBody = Some("{}"))
  val optionRequest = buildRequest(path = "/", method = "OPTION")
  val optionResponse = buildResponse(headers = Map("Option" -> "Value-X"))

  override val pactFragment = buildPactFragment(
    consumer = consumer,
    provider = provider,
    interactions = List(
      buildInteraction("a request for foo", List(), fooRequest, fooResponse),
      buildInteraction("an option request", List(), optionRequest, optionResponse)
    )
  )

  pactFragment.description >> {
    "GET returns a 200 status and empty body" >> {
      val simpleGet = ConsumerService(providerConfig.url).simpleGet("/foo")
      Await.result(simpleGet, timeout) must be_==(200, "{}")
    }

    "OPTION returns a 200 status and the correct headers" >> {
      val optionsResult = ConsumerService(providerConfig.url).options("/")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Content-Length" -> "0", "Connection" -> "keep-alive", "Option" -> "Value-X"))
    }
  }
}
