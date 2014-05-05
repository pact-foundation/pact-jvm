package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.consumer.PactVerification.PactVerified
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import au.com.dius.pact.consumer.Fixtures.ConsumerService
import au.com.dius.pact.model.{Request, PactFragment}

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
      awaitResult(provider
        .uponReceiving("request for root path").matching("/")
        .willRespondWith(200, Map("foo" -> "bar"), "[]")
        .duringConsumerSpec { config: MockProviderConfig =>
          awaitResult(ConsumerService(config.url).simpleGet("/")) must beEqualTo(200, Some("[]"))
        }) must beEqualTo(PactVerified)
    }

    "longest request" in {
      awaitResult(provider
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
          awaitResult(ConsumerService(config.url).extractResponseTest()) must beTrue
        }) must beEqualTo(PactVerified)
    }

    "multiple requests" in {
      awaitResult(provider
        .uponReceiving("request for root path").matching("/")
        .willRespondWith(200, Map("foo" -> "bar"), "[]")
        .uponReceiving("request for foo path").matching("/foo")
        .willRespondWith(200, Map[String, String](), "{}")
        .duringConsumerSpec { config: MockProviderConfig =>
          awaitResult(ConsumerService(config.url).simpleGet("/")) must beEqualTo(200, Some("[]"))
          awaitResult(ConsumerService(config.url).simpleGet("/foo")) must beEqualTo(200, Some("{}"))
      }) must beEqualTo(PactVerified)
    }
  }

  "specific state" should {
    val providerInSpecificState = provider.given(interaction.providerState)

    "specificRequest" in {
      awaitResult(providerInSpecificState
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
          awaitResult(ConsumerService(config.url).extractResponseTest()) must beTrue
        }) must beEqualTo(PactVerified)
    }
  }
}