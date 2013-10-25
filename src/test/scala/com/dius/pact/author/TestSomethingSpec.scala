package com.dius.pact.author

import org.specs2._
import com.dius.pact.model._
import com.dius.pact.model.MakeInteraction.given

class TestSomethingSpec extends mutable.Specification {
  "JSON Thingy" should {
    "output valid JSON" in {
      val jsonResult = new JSONThingy().convert(Map("cat" -> List()))
      jsonResult must beEqualTo("{\"cat\":[]}")
    }
  }

  "pact" should {
    "be constructed" in {
      val pact:Pact = MakePact()
        .withProvider("p")
        .withConsumer("c")
        .withInteractions(
          given("something")
          .uponReceiving("lol")
        )

      pact.provider.name must beEqualTo("p")
      pact.consumer.name must beEqualTo("c")
      pact.interactions.size must beEqualTo(1)
      pact.interactionFor("something", "lol") must not be None
    }
  }
}