package com.dius.pact.author

import org.specs2._
import com.dius.pact.model._
import com.dius.pact.model.MakeInteraction.given
import Fixtures._
import play.api.libs.json.Json

class PactBuilderSpec extends mutable.Specification {
  "Pact Builder" should {
    "construct a pact" in {
      val pact:Pact = MakePact()
        .withProvider(provider.name)
        .withConsumer(consumer.name)
        .withInteractions(
          given(interaction.providerState)
          .uponReceiving( description = interaction.description,
                          path = request.path,
                          method = request.method,
                          headers = request.headers,
                          body = Some(Json.parse(request.body.get))
          )
          .willRespondWith(status=200)
        )

      pact must beEqualTo(Fixtures.pact)
    }
  }
}