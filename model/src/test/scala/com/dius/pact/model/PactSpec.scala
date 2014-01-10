package com.dius.pact.model

import org.specs2.mutable.Specification

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
  }
}
