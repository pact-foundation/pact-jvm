package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.consumer.PactVerification.PactVerified
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import au.com.dius.pact.consumer.Fixtures.ConsumerService
import au.com.dius.pact.model.PactFragment

/**
 * This is what a consumer pact should roughly look like
 */
class ConsumerPactSpec extends Specification {

  sequential

  def awaitResult[A](f: Future[A]): A = {
    Await.result(f, Duration(10, "s"))
  }

  val provider = PactFragment
    .consumer(consumer.name)
    .hasPactWith(Fixtures.provider.name)

  "default state" should {

    "minimal request" in {
      provider
        .uponReceiving("request for root path").matching("/")
        .willRespondWith(200, Map("foo" -> "bar"), "[]")
        .duringConsumerSpec { config: MockProviderConfig =>
          awaitResult(ConsumerService(config.url).hitEndpoint) must beTrue
        } must beEqualTo(PactVerified)
    }

    "longest request" in {
        provider
          .uponReceiving(interaction.description).matching(
            path = request.path,
            method = request.method,
            headers = request.headers,
            body = request.body)
          .willRespondWith(
            status = 200,
            headers = response.headers.get,
            body = response.bodyString.get)
          .duringConsumerSpec { config: MockProviderConfig =>
            awaitResult(ConsumerService(config.url).hitEndpoint) must beTrue
          } must beEqualTo(PactVerified)
    }
  }

  "specific state" should {
    val providerInSpecificState = provider.given(interaction.providerState)

    "specificRequest" in {
      providerInSpecificState
        .uponReceiving(interaction.description).matching(
          path = request.path,
          method = request.method,
          headers = request.headers,
          body = request.body)
        .willRespondWith(
          status = 200,
          headers = response.headers.get,
          body = response.bodyString.get)
        .duringConsumerSpec { config: MockProviderConfig =>
          awaitResult(ConsumerService(config.url).hitEndpoint) must beTrue
        } must beEqualTo(PactVerified)
    }
  }
}