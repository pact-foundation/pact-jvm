package au.com.dius.pact.model

import org.specs2.mutable.Specification
import au.com.dius.pact.model.Pact.MergeSuccess
import au.com.dius.pact.model.Pact.ConflictingInteractions

class PactSpec extends Specification {
  "Pact" should {
    "Locate Interactions" in {
      val description = "descriptiondata"
      val state = "stateData"

      val interaction = Interaction(
        description,
        state,
        Request(HttpMethod.Get,"",None, None),
        Response(200, None, None)
      )

      val pact = Pact(
        Provider("foo"),
        Consumer("bar"),
        Seq(interaction)
      )

      pact.interactionFor(description, state) must beSome(interaction)
    }

    "merge" should {
      import Fixtures._

      "allow different descriptions" in {
        val newInteractions = Seq(interaction.copy(description = "different"))
        val result = Pact.merge(pact, pact.copy(interactions = newInteractions))
        result must beEqualTo(MergeSuccess(Pact(provider, consumer, interactions ++ newInteractions )))
      }

      "allow different states" in {
        val newInteractions = Seq(interaction.copy(providerState = "different"))
        val result = Pact.merge(pact, pact.copy(interactions = newInteractions))
        result must beEqualTo(MergeSuccess(Pact(provider, consumer, interactions ++ newInteractions )))
      }

      "allow identical interactions without duplication" in {
        val result = Pact.merge(pact, pact.copy())
        result must beEqualTo(MergeSuccess(pact))
      }

      "refuse different requests for identical description and states" in {
        val newInteractions = Seq(interaction.copy(request = request.copy(path = "different")))
        val result = Pact.merge(pact, pact.copy(interactions = newInteractions))
        result must beEqualTo(ConflictingInteractions(Seq((interaction, newInteractions.head))))
      }

      "refuse different responses for identical description and states" in {
        val newInteractions = Seq(interaction.copy(response = response.copy(status = 503)))
        val result = Pact.merge(pact, pact.copy(interactions = newInteractions))
        result must beEqualTo(ConflictingInteractions(Seq((interaction, newInteractions.head))))
      }
    }
  }
}
