package com.dius.pact.consumer

import org.specs2.mutable.Specification
import com.dius.pact.model.{Pact, MakePact}
import com.dius.pact.author.Fixtures._
import com.dius.pact.model.MakeInteraction._
import com.dius.pact.author.PactServerConfig
import ConsumerPact._
import akka.actor.ActorSystem
import com.dius.pact.consumer.PactVerification.PactVerified

/**
 * This is what a consumer pact should roughly look like
 */
class ConsumerPactSpec extends Specification {
  implicit val actorSystem = ActorSystem()
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
          .willRespondWith(status = 200, headers = response.headers, body = response.body)
    )

    "Run integration tests" in {
      //TODO: make port selection automatic so that mutiple specs can run in parallel
      val config = PactServerConfig(port = 9998)
      pact.runConsumer(config, interaction.providerState) {
        ConsumerService(config.url).hitEndpoint must beTrue.await
      } must beEqualTo(PactVerified).await

      //TODO: use environment property for pact output folder
      val saved: String = io.Source.fromFile(s"target/pacts/${pact.consumer.name}-${pact.provider.name}.json").mkString
      val savedPact = Pact.from(saved)

      //TODO: update expected string when pact serialization is complete
      savedPact must beEqualTo(pact)
    }
  }
}
