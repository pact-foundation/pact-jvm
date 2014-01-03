package com.dius.pact.author

import org.specs2.mutable.Specification
import Fixtures._
import com.dius.pact.model.{Request, Response}

class RequestMatchingSpec extends Specification {
  "matching" should {
    def test(r:Request):Either[Response, String] = RequestMatching(pact).matchRequest(r)

    "match the valid request" in {
      test(request) must beEqualTo(Left(response))
    }

    "disallow additional keys" in {
      val leakyRequest = request.copy(body = request.body.map{_.replaceFirst("\\{", """{"extra": 1, """)})
      test(leakyRequest) must beEqualTo(Right(s"unexpected request"))
    }

    "require precise matching" in {
      val impreciseRequest = request.copy(body = request.body.map{_.replaceFirst("true", "false")})
      test(impreciseRequest) must beEqualTo(Right(s"unexpected request"))
    }

    "trim protocol, server name and port" in {
      val fancyRequest = request.copy(path = "http://localhost:9090/")
      test(fancyRequest) must beEqualTo(Left(response))
    }

    "fail to match when missing headers" in {
      val headerlessRequest = request.copy(headers = Some(Map()))
      test(headerlessRequest) must beEqualTo(Right(s"unexpected request"))
    }

    "fail to match on header with incorrect value" in {
      val wrongHeaderRequest = request.copy(headers = Some(Map("testreqheader" -> "WRANG!")))
      test(wrongHeaderRequest) must beEqualTo(Right(s"unexpected request"))
    }

    "allow additional headers" in {
      val extraHeaderRequest = request.copy(headers = request.headers.map(_.+("additonal" -> "header")))
      test(extraHeaderRequest) must beEqualTo(Left(response))
    }
  }
}
