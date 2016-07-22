package au.com.dius.pact.consumer.specs2

import java.util.concurrent.TimeUnit._

import au.com.dius.pact.consumer._
import au.com.dius.pact.model.{MockProviderConfig, PactConfig, PactSpecVersion}
import org.junit.runner.RunWith
import org.specs2.execute.{AsResult, Result, Success}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AroundEach

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class MultipleExamplesPactSpec extends Specification with PactSpec with AroundEach {
  sequential
  override val provider: String = "SpecsProvider"
  override val consumer: String = "SpecsConsumer"

  val timeout = Duration(5000, MILLISECONDS)

  val pact = uponReceiving("a request for foo")
    .matching(path = "/foo")
    .willRespondWith(body = "{}")
    .uponReceiving("an option request")
    .matching(path = "/", method = "OPTION")
    .willRespondWith(headers = Map("Option" -> "Value-X"))
    .asPactFragment()
  val providerConfig = MockProviderConfig.createDefault(PactConfig(PactSpecVersion.V2))

  pact.description >> {
    "GET returns a 200 status and empty body" >> {
      val simpleGet = ConsumerService(providerConfig.url).simpleGet("/foo")
      val optionsResult = ConsumerService(providerConfig.url).options("/")
      Await.result(simpleGet, timeout) must be_==(200, "{}")
    }

    "OPTION returns a 200 status and the correct headers" >> {
      val simpleGet = ConsumerService(providerConfig.url).simpleGet("/foo")
      val optionsResult = ConsumerService(providerConfig.url).options("/")
      Await.result(optionsResult, timeout) must be_==(200, "",
        Map("Content-Length" -> "0", "Connection" -> "keep-alive", "Option" -> "Value-X"))
    }

  }

  def around[R: AsResult](r: => R): Result = {
    VerificationResultAsResult().asResult(pact.duringConsumerSpec(providerConfig)(r, (u: R) => u match {
      case Success => Some(u)
      case _ => None
    }))
  }

}
