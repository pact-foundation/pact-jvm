package com.dius.pact.author

import org.specs2._
import com.dius.pact.model._
import com.dius.pact.model.MakeInteraction.given
import play.api.libs.json.Json

class PactBuilderSpec extends mutable.Specification {
  "Pact Builder" should {
    "construct a pact" in {
      val pact:Pact = MakePact()
        .withProvider("p")
        .withConsumer("c")
        .withInteractions(
          given("something")
          .uponReceiving(description = "test", path = "/")
          .willRespondWith(status=200)
        )

      pact.provider.name must beEqualTo("p")
      pact.consumer.name must beEqualTo("c")
      pact.interactions.size must beEqualTo(1)

      val interaction = pact.interactions.head
      interaction.description must beEqualTo("test")
      interaction.providerState must beEqualTo("something")
      interaction.request.path must beEqualTo("/")
      interaction.request.method must beEqualTo("GET")
      interaction.response.status must beEqualTo(200)
    }
  }
}