package com.dius.pact.consumer

import org.specs2.mutable.Specification
import com.dius.pact.model.{Pact, MakePact}
import com.dius.pact.author.Fixtures._
import com.dius.pact.model.MakeInteraction._
import com.dius.pact.author.PactServerConfig
import ConsumerPact._
import akka.actor.ActorSystem
import com.dius.pact.consumer.PactVerification.{ConsumerTestsFailed, PactVerified}

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


    "Report test success and write pact" in {
      val config = PactServerConfig(port = 9988)
      //TODO: make port selection automatic so that mutiple specs can run in parallel
      pact.runConsumer(config, interaction.providerState) {
        ConsumerService(config.url).hitEndpoint must beTrue.await
      } must beEqualTo(PactVerified).await

      //TODO: use environment property for pact output folder
      val saved: String = io.Source.fromFile(s"target/pacts/${pact.consumer.name}-${pact.provider.name}.json").mkString
      val savedPact = Pact.from(saved)

      //TODO: update expected string when pact serialization is complete
      savedPact must beEqualTo(pact)
    }

    "Report test failure nicely" in {
      val error = new RuntimeException("bad things happened in the test!")
      val config = PactServerConfig(port = 9987)
      pact.runConsumer(config, interaction.providerState) {
        throw error
      } must beEqualTo(ConsumerTestsFailed(error)).await
    }
  }
}