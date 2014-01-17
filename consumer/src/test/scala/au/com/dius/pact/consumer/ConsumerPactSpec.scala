package au.com.dius.pact.consumer

import org.specs2.mutable.Specification
import au.com.dius.pact.model.{Pact}
import au.com.dius.pact.consumer.{Fixtures, PactServerConfig}
import Fixtures._
import au.com.dius.pact.model.{MakePact, MakeInteraction}
import MakeInteraction._
import ConsumerPact._
import akka.actor.ActorSystem
import au.com.dius.pact.consumer.PactVerification.{PactWritten, ConsumerTestsFailed, PactVerified}
import au.com.dius.pact.consumer.PactServerConfig
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}

/**
 * This is what a consumer pact should roughly look like
 */
class ConsumerPactSpec extends Specification {

  isolated

  implicit val actorSystem = ActorSystem("test-client")
  implicit val executionContext = actorSystem.dispatcher

  "consumer pact" should {
    val pact: Pact = MakePact()
    .withProvider(provider.name)
    .withConsumer(consumer.name)
    .withInteractions(
      given(interaction.providerState)
    .uponReceiving(
        description = interaction.description,
        path = request.path,
        method = request.method,
        headers = request.headers,
        body = request.body)
    .willRespondWith(status = 200, headers = response.headers, body = response.body))

    def awaitResult[A](f: Future[A]): A = {
      Await.result(f, Duration(10, "s"))
    }

    "Report test success and write pact" in {
      val config = PactServerConfig(port = 9988)


      //TODO: make port selection automatic so that mutiple specs can run in parallel
      awaitResult(pact.runConsumer(config, interaction.providerState) {
        awaitResult(ConsumerService(config.url).hitEndpoint) must beTrue
      }) must beEqualTo(PactVerified)

      //TODO: use environment property for pact output folder
      val saved: String = io.Source.fromFile(s"target/pacts/${pact.consumer.name}-${pact.provider.name}.json").mkString
      val savedPact = Pact.from(saved)

      //TODO: update expected string when pact serialization is complete
      savedPact must beEqualTo(pact)
    }

    "Report test failure nicely" in {
      val error = new RuntimeException("bad things happened in the test!")
      val config = PactServerConfig(port = 9987)
      awaitResult(pact.runConsumer(config, interaction.providerState) {
        throw error
      }) must beEqualTo(ConsumerTestsFailed(error))
    }
  }
}