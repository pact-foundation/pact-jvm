package au.com.dius.pact.model

import au.com.dius.pact.model.Fixtures._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MatchingSpec extends Specification {
  "Matching" should {
    import au.com.dius.pact.model.Matching._
    implicit val autoParse = JsonDiff.autoParse _

    "Body Matching" should {
      val config = DiffConfig()

      "Handle both None" in {
        matchBody(Request("", "", None, Some(Map("Content-Type" -> "a")), None, None),
          Request("", "", None, Some(Map("Content-Type" -> "a")), None, None), config) must beEmpty
      }

      "Handle left None" in {
        val expected = List(BodyMismatch(request.body, None))
        matchBody(Request("", "", None, Some(Map("Content-Type" -> "a")), request.body, None),
          Request("", "", None, Some(Map("Content-Type" -> "a")), None, None), config) must beEqualTo(expected)
      }

      "Handle right None" in {
        matchBody(Request("", "", None, Some(Map("Content-Type" -> "a")), None, None),
          Request("", "", None, Some(Map("Content-Type" -> "a")), request.body, None), config) must beEmpty
      }

      "Handle different mime types" in {
        val expected = List(BodyTypeMismatch("a", "b"))
        matchBody(Request("", "", None, Some(Map("Content-Type" -> "a")), request.body, None),
          Request("", "", None, Some(Map("Content-Type" -> "b")), request.body, None), config) must beEqualTo(expected)
      }

      "match different mimetypes by regexp" in {
        matchBody(Request("", "", None, Some(Map("Content-Type" -> "application/x+json")), Some("{ \"name\":  \"bob\" }"), None),
          Request("", "", None, Some(Map("Content-Type" -> "application/x+json")), Some("{\"name\":\"bob\"}"), None), config) must beEmpty
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
      "match same"  in {
        matchQuery(Some("a=b"), Some("a=b")) must beNone
      }

      "match none" in {
        matchQuery(None, None) must beNone
      }

      "mismatch none to something" in {
        matchQuery(None, Some("a=b")) must beSome(QueryMismatch("", "a=b"))
      }

      "mismatch something to none" in {
        matchQuery(Some("a=b"), None) must beSome(QueryMismatch("a=b", ""))
      }

      "match keys in different order"  in {
        matchQuery(Some("status=RESPONSE_RECEIVED&insurerCode=ABC"), Some("insurerCode=ABC&status=RESPONSE_RECEIVED")) must beNone
      }

      "mismatch if the same key is repeated with values in different order"  in {
        matchQuery(Some("a=1&a=2&b=3"), Some("a=2&a=1&b=3")) must beSome(QueryMismatch("a=1&a=2&b=3", "a=2&a=1&b=3"))
      }
    }

    "Header Matching" should {

      "match empty" in {
        matchHeaders(Request("", "", None, None, None, None),
          Request("", "", None, None, None, None)) must beEmpty
      }

      "match same headers" in {
        matchHeaders(Request("", "", None, Some(Map("A" -> "B")), None, None),
          Request("", "", None, Some(Map("A" -> "B")), None, None)) must beEmpty
      }

      "ignore additional headers" in {
        matchHeaders(Request("", "", None, Some(Map("A" -> "B")), None, None),
          Request("", "", None, Some(Map("A" -> "B", "C" -> "D")), None, None)) must beEmpty
      }

      "complain about missing headers" in {
        matchHeaders(Request("", "", None, Some(Map("A" -> "B", "C" -> "D")), None, None),
          Request("", "", None, Some(Map("A" -> "B")), None, None)) must beEqualTo(List(
          HeaderMismatch("C", "D", "", Some("Expected a header 'C' but was missing"))))
      }

      "complain about incorrect headers" in {
        matchHeaders(Request("", "", None, Some(Map("A" -> "B")), None, None),
          Request("", "", None, Some(Map("A" -> "C")), None, None)) must beEqualTo(List(
          HeaderMismatch("A", "B", "C", Some("Expected header 'A' to have value 'B' but was 'C'"))))
      }

    }
  }
}
