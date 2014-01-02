package com.dius.pact.author

import org.specs2.mutable.Specification
import Fixtures._
import scala.util.Failure

class RequestMatchingSpec extends Specification {
  "matching" should {
    "match the valid request" in {
      RequestMatching(pact).matchRequest(request) must beEqualTo(Left(response))
    }

    "disallow additional keys" in {
      val leakyRequest = request.copy(body = request.body.map{_.replaceFirst("\\{", """{"extra": 1, """)})
      RequestMatching(pact).matchRequest(leakyRequest) must beEqualTo(Right(s"unexpected request $leakyRequest"))
    }

    "require precise matching" in {
      val impreciseRequest = request.copy(body = request.body.map{_.replaceFirst("true", "false")})
      RequestMatching(pact).matchRequest(impreciseRequest) must beEqualTo(Right(s"unexpected request $impreciseRequest"))
    }

    "trim protocol, server name and port" in {
      val fancyRequest = request.copy(path = "http://localhost:9090/")
      RequestMatching(pact).matchRequest(fancyRequest) must beEqualTo(Left(response))
    }
  }
}
