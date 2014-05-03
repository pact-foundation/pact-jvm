package au.com.dius.pact.model

import org.specs2.mutable.Specification

class ResponseMatchingSpec extends Specification {
  "response matching" should {
    import Matching._
    "match statuses" in {
      matchStatus(200, 200) must beNone
    }

    "mismatch statuses" in {
      matchStatus(200, 300) must beSome(StatusMismatch(200, 300))
    }
  }
}
