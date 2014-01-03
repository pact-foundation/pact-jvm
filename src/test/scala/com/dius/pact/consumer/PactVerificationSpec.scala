package com.dius.pact.consumer

import org.specs2.mutable.Specification
import com.dius.pact.author.Fixtures._
import org.specs2.mock.Mockito
import com.dius.pact.model.{Response, Interaction}
import com.dius.pact.consumer.PactVerification._

class PactVerificationSpec extends Specification with Mockito {
  "PactVerification" should {
    def test(actualInteractions: Seq[Interaction], expectedResult: VerificationResult) = {
      PactVerification(pact.interactions, actualInteractions) must beEqualTo(expectedResult)
    }

    val unexpectedInteraction = Interaction("", "", request.copy(path = "unexpected"), Response.invalidRequest)

    "complain about missing interactions" in {
      test(
        Seq(),
        MissingInteractions(pact.interactions)
      )
    }

    "complain about unexpected interactions" in {
      test(
        interactions :+ unexpectedInteraction,
        UnexpectedInteractions(Seq(unexpectedInteraction))
      )
    }

    "complain about both failures" in {
      val actual = Seq(unexpectedInteraction)
      test(
        actual,
        PactFailure(missing = pact.interactions, unexpected = actual)
      )
    }

    "pass successful runs" in {
      test(
        interactions.map(_.copy(description="MockServiceProvider received")),
        PactVerified
      )
    }
  }
}
