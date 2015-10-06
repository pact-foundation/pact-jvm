package au.com.dius.pact.model

import au.com.dius.pact.model.Fixtures._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MatchingSpec extends Specification {

  val emptyQuery = None

  "Matching" should {
    import au.com.dius.pact.model.Matching._

    "Body Matching" should {
      val config = DiffConfig()

      "Handle both None" in {
        matchBody(Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), None, None),
          Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), None, None), config) must beEmpty
      }

      "Handle left None" in {
        val expected = List(BodyMismatch(request.body, None))
        matchBody(Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), request.body, None),
          Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), None, None), config) must beEqualTo(expected)
      }

      "Handle right None" in {
        matchBody(Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), None, None),
          Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), request.body, None), config) must beEmpty
      }

      "Handle different mime types" in {
        val expected = List(BodyTypeMismatch("a", "b"))
        matchBody(Request("", "", emptyQuery, Some(Map("Content-Type" -> "a")), request.body, None),
          Request("", "", emptyQuery, Some(Map("Content-Type" -> "b")), request.body, None), config) must beEqualTo(expected)
      }

      "match different mimetypes by regexp" in {
        matchBody(Request("", "", emptyQuery, Some(Map("Content-Type" -> "application/x+json")), Some("{ \"name\":  \"bob\" }"), None),
          Request("", "", emptyQuery, Some(Map("Content-Type" -> "application/x+json")), Some("{\"name\":\"bob\"}"), None), config) must beEmpty
      }

    }

    "Method Matching" should {
      "match same"  in {
        matchMethod("a", "a") must beNone
      }

      "match ignore case" in {
        matchMethod("a", "A") must beNone
      }

      "mismatch different" in {
        matchMethod("a", "b") must beSome(MethodMismatch("a", "b"))
      }
    }

    "Query Matching" should {

      def query(queryString: String = "") = Request("", "", queryString, Map[String, String](), "", Map[String, Map[String, Any]]())

      "match same"  in {
        matchQuery(query("a=b"), query("a=b")) must beEmpty
      }

      "match none" in {
        matchQuery(query(), query()) must beEmpty
      }

      "mismatch none to something" in {
        matchQuery(query(), query("a=b")) must beEqualTo(Seq(
          QueryMismatch("a", "", "b", Some("Unexpected query parameter 'a' received"), "$.query.a")))
      }

      "mismatch something to none" in {
        matchQuery(query("a=b"), query()) must beEqualTo(Seq(
          QueryMismatch("a", "b", "", Some("Expected query parameter 'a' but was missing"), "$.query.a")))
      }

      "match keys in different order"  in {
        matchQuery(query("status=RESPONSE_RECEIVED&insurerCode=ABC"), query("insurerCode=ABC&status=RESPONSE_RECEIVED")) must beEmpty
      }

      "mismatch if the same key is repeated with values in different order"  in {
        matchQuery(query("a=1&a=2&b=3"), query("a=2&a=1&b=3")) must beEqualTo(
          Seq(QueryMismatch("a", "1", "2", Some("Expected '1' but received '2' for query parameter 'a'"), "$.query.a.0"),
              QueryMismatch("a", "2", "1", Some("Expected '2' but received '1' for query parameter 'a'"), "$.query.a.1")))
      }
    }

    "Header Matching" should {

      "match empty" in {
        matchHeaders(Request("", "", emptyQuery, None, None, None),
          Request("", "", emptyQuery, None, None, None)) must beEmpty
      }

      "match same headers" in {
        matchHeaders(Request("", "", emptyQuery, Some(Map("A" -> "B")), None, None),
          Request("", "", emptyQuery, Some(Map("A" -> "B")), None, None)) must beEmpty
      }

      "ignore additional headers" in {
        matchHeaders(Request("", "", emptyQuery, Some(Map("A" -> "B")), None, None),
          Request("", "", emptyQuery, Some(Map("A" -> "B", "C" -> "D")), None, None)) must beEmpty
      }

      "complain about missing headers" in {
        matchHeaders(Request("", "", emptyQuery, Some(Map("A" -> "B", "C" -> "D")), None, None),
          Request("", "", emptyQuery, Some(Map("A" -> "B")), None, None)) must beEqualTo(List(
          HeaderMismatch("C", "D", "", Some("Expected a header 'C' but was missing"))))
      }

      "complain about incorrect headers" in {
        matchHeaders(Request("", "", emptyQuery, Some(Map("A" -> "B")), None, None),
          Request("", "", emptyQuery, Some(Map("A" -> "C")), None, None)) must beEqualTo(List(
          HeaderMismatch("A", "B", "C", Some("Expected header 'A' to have value 'B' but was 'C'"))))
      }

    }
  }
}
