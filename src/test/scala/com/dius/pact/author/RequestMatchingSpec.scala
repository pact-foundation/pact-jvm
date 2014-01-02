package com.dius.pact.author

import org.specs2.mutable.Specification
import Fixtures._
import scala.util.Failure

class RequestMatchingSpec extends Specification {
  "matching" should {
    "match the valid request" in {
      RequestMatching(pact).matchRequest(request) must beSuccessfulTry.withValue(response)
    }

    "disallow additional keys" in {
      val leakyRequest = request.copy(body = request.body.map{_.replaceFirst("\\{", """{"extra": 1, """)})
      RequestMatching(pact).matchRequest(leakyRequest) must beEqualTo(Failure(MatchFailure(s"unexpected request $leakyRequest")))
    }

    "require precise matching" in {
      val impreciseRequest = request.copy(body = request.body.map{_.replaceFirst("true", "false")})
      RequestMatching(pact).matchRequest(impreciseRequest) must beEqualTo(Failure(MatchFailure(s"unexpected request $impreciseRequest")))
    }
  }
}
