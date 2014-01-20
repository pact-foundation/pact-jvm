package au.com.dius.pact.model

import org.specs2.mutable.Specification
import Fixtures._
import org.json4s.JsonDSL._

class RequestMatchingSpec extends Specification {
  "request matching" should {
    def test(r:Request):Option[Response] = RequestMatching(pact.interactions).findResponse(r)

    "match the valid request" in {
      test(request) must beSome(response)
    }

    "disallow additional keys" in {
      val leakyRequest = request.copy(body = Some(Map("test" -> true, "extra" -> false)))
      test(leakyRequest) must beNone
    }

    "require precise matching" in {
      val impreciseRequest = request.copy(body = Some("test" -> false))
      test(impreciseRequest) must beNone
    }

    "trim protocol, server name and port" in {
      val fancyRequest = request.copy(path = "http://localhost:9090/")
      test(fancyRequest) must beSome(response)
    }

    "fail to match when missing headers" in {
      val headerlessRequest = request.copy(headers = Some(Map()))
      test(headerlessRequest) must beNone
    }

    "fail to match on header with incorrect value" in {
      val wrongHeaderRequest = request.copy(headers = Some(Map("testreqheader" -> "WRANG!")))
      test(wrongHeaderRequest) must beNone
    }

    "allow additional headers" in {
      val extraHeaderRequest = request.copy(headers = request.headers.map(_.+("additonal" -> "header")))
      test(extraHeaderRequest) must beSome(response)
    }
  }
}
