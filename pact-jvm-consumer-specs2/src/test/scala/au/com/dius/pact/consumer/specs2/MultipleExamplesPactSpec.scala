package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit._

import au.com.dius.pact.consumer._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class MultipleExamplesPactSpec extends Specification with PactSpec with UnitSpecsSupport {
  sequential

  override val provider: String = "SpecsProvider"
  override val consumer: String = "SpecsConsumer"

  val timeout = Duration(5000, MILLISECONDS)

  override val pactFragment = uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(maybeBody = Some("{}"))
    .uponReceiving("an option request")
    .matching(path = "/", method = "OPTION")
    .willRespondWith(headers = Map("Option" -> List("Value-X")))
    .asPactFragment()

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
