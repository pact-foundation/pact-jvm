package au.com.dius.pact.model

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.JavaConversions

@RunWith(classOf[JUnitRunner])
class MatchingSpec extends Specification {
  import Fixtures._

  val emptyQuery = null

  "Matching" should {
    import au.com.dius.pact.model.Matching._

    "Body Matching" should {
      val config = DiffConfig()

      "Handle both None" in {
        matchBody(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a"))), config) must beEmpty
      }

      "Handle left None" in {
        val expected = List(BodyMismatch(request.getBody, None))
        matchBody(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a")), request.getBody),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a"))), config) must beEqualTo(expected)
      }

      "Handle right None" in {
        matchBody(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a")), request.getBody), config) must beEmpty
      }

      "Handle different mime types" in {
        val expected = List(BodyTypeMismatch("a", "b"))
        matchBody(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "a")), request.getBody),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "b")), request.getBody), config) must beEqualTo(expected)
      }

      "match different mimetypes by regexp" in {
        matchBody(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "application/x+json")), "{ \"name\":  \"bob\" }"),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("Content-Type" -> "application/x+json")), "{\"name\":\"bob\"}"), config) must beEmpty
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

      def query(queryString: String = "") = new Request("", "", PactReader.queryStringToMap(queryString), null, "", null)

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
        matchHeaders(new Request("", "", emptyQuery),
          new Request("", "", emptyQuery)) must beEmpty
      }

      "match same headers" in {
        matchHeaders(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B")))) must beEmpty
      }

      "ignore additional headers" in {
        matchHeaders(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B", "C" -> "D")))) must beEmpty
      }

      "complain about missing headers" in {
        matchHeaders(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B", "C" -> "D"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B")))) must beEqualTo(List(
          HeaderMismatch("C", "D", "", Some("Expected a header 'C' but was missing"))))
      }

      "complain about incorrect headers" in {
        matchHeaders(new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "B"))),
          new Request("", "", emptyQuery, JavaConversions.mapAsJavaMap(Map("A" -> "C")))) must beEqualTo(List(
          HeaderMismatch("A", "B", "C", Some("Expected header 'A' to have value 'B' but was 'C'"))))
      }

    }
  }
}
