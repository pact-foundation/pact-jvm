package au.com.dius.pact.model

import org.specs2.mutable.Specification
import Fixtures._
import org.json4s.JsonDSL._
import au.com.dius.pact.model.HttpMethod._
import scala.Some

class RequestMatchingSpec extends Specification {

  "request matching" should {

    def test(actual: Request): Option[Response] = RequestMatching(pact.interactions).findResponse(actual)

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

    "allow additional headers" in {
      val extraHeaderRequest = request.copy(headers = request.headers.map(_.+("additonal" -> "header")))
      test(extraHeaderRequest) must beSome(response)
    }

  }

  "request with cookie" should {

    val request = Request(Get, "/", Map("Cookie" -> "key1=value1;key2=value2"), "")
    val interactions = List(interaction.copy(request = request))

    def test(r: Request): Option[Response] = RequestMatching(interactions).findResponse(r)

    "match if actual cookie exactly matches the expected" in {
      val cookieRequest = Request(Get, "/", Map("Cookie" -> "key1=value1;key2=value2"), "")
      test(cookieRequest) must beSome(response)
    }

    "mismatch if actual cookie contains less data than expected cookie" in {
      val cookieRequest = Request(Get, "/", Map("Cookie" -> "key2=value2"), "")
      test(cookieRequest) must beNone
    }

    "match if actual cookie contains more data than expected one" in {
      val cookieRequest = Request(Get, "/", Map("Cookie" -> "key2=value2;key1=value1;key3=value3"), "")
      test(cookieRequest) must beSome(response)
    }

    "mismatch if actual cookie has no intersection with expected request" in {
      val cookieRequest = Request(Get, "/", Map("Cookie" -> "key5=value5"), "")
      test(cookieRequest) must beNone
    }

    "match when cookie field is different from cases" in {
      val cookieRequest = Request(Get, "/", Map("cOoKie" -> "key1=value1;key2=value2"), "")
      test(cookieRequest) must beSome(response)
    }

    "match when there are spaces between cookie items" in {
      val cookieRequest = Request(Get, "/", Map("cookie" -> "key1=value1; key2=value2"), "")
      test(cookieRequest) must beSome(response)
    }
  }

}
