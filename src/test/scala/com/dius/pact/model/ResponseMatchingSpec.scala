package com.dius.pact.model

import org.specs2.mutable.Specification

class ResponseMatchingSpec extends Specification {
  "matching" should {
    import Matching._
    "match statuses" in {
      matchStatus(200, 200) must beEqualTo(MatchFound)
    }

    "mismatch statuses" in {
      matchStatus(200, 300) must beEqualTo(StatusMismatch(200, 300))
    }
  }
}
