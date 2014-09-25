package au.com.dius.pact.model

import Fixtures._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.pretty
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RequestMatchingSpec extends Specification {

  "request matching" should {

    def test(actual: Request): Option[Response] = RequestMatching(pact.interactions).findResponse(actual)

    "match the valid request" in {
      test(request) must beSome(response)
    }

    "disallow additional keys" in {
      val leakyRequest = request.copy(body = Some(pretty(map2jvalue(Map("test" -> true, "extra" -> false)))))
      test(leakyRequest) must beNone
    }

    "require precise matching" in {
      val impreciseRequest = request.copy(body = Some(pretty(map2jvalue(Map("test" -> false)))))
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

    "fail to match when headers are present but contain incorrect value" in {
      val incorrectRequest = request.copy(headers = Some(Map("testreqheader" -> "incorrectValue")))
      test(incorrectRequest) must beNone
    }

    "allow additional headers" in {
      val extraHeaderRequest = request.copy(headers = request.headers.map(_.+("additonal" -> "header")))
      test(extraHeaderRequest) must beSome(response)
    }

    "allow query string in different order" in {
      val queryRequest = request.copy(query = Some("r=s&q=p&q=p2"))
      test(queryRequest) must beSome(response)
    }

    "fail if query string has the same parameter repeated in different order" in {
      val queryRequest = request.copy(query = Some("r=s&q=p2&q=p"))
      test(queryRequest) must beNone
    }

  }

  "request with cookie" should {

    val request = Request(HttpMethod.Get, "/", null, Map("Cookie" -> "key1=value1;key2=value2"), "", null)
    val interactions = List(interaction.copy(request = request))

    def test(r: Request): Option[Response] = RequestMatching(interactions).findResponse(r)

    "match if actual cookie exactly matches the expected" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("Cookie" -> "key1=value1;key2=value2"), "", null)
      test(cookieRequest) must beSome(response)
    }

    "mismatch if actual cookie contains less data than expected cookie" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("Cookie" -> "key2=value2"), "", null)
      test(cookieRequest) must beNone
    }

    "match if actual cookie contains more data than expected one" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("Cookie" -> "key2=value2;key1=value1;key3=value3"), "", null)
      test(cookieRequest) must beSome(response)
    }

    "mismatch if actual cookie has no intersection with expected request" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("Cookie" -> "key5=value5"), "", null)
      test(cookieRequest) must beNone
    }

    "match when cookie field is different from cases" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("cOoKie" -> "key1=value1;key2=value2"), "", null)
      test(cookieRequest) must beSome(response)
    }

    "match when there are spaces between cookie items" in {
      val cookieRequest = Request(HttpMethod.Get, "/", null, Map("cookie" -> "key1=value1; key2=value2"), "", null)
      test(cookieRequest) must beSome(response)
    }
  }

}
