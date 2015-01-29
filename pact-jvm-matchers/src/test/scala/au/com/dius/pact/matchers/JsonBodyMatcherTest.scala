package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, DiffConfig, Request}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations
import org.specs2.matcher.Expectable

@RunWith(classOf[JUnitRunner])
class JsonBodyMatcherTest extends Specification with AllExpectations {
  isolated

  var expectedBody: Option[String] = None
  var actualBody: Option[String] = None
  var matchers: Option[Map[String, Map[String, String]]] = None
  val expected = () => Request("", "", None, None, expectedBody, matchers)
  val actual = () => Request("", "", None, None, actualBody, None)

  var diffconfig = DiffConfig(structural = true)

  "matching json bodies" should {

    val matcher = new JsonBodyMatcher()

    "return no mismatches" should {

      "when comparing empty bodies" in {
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when comparing an empty body to anything" in {
        actualBody = Some("Blah")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal bodies" in {
        actualBody = Some("\"Blah\"")
        expectedBody = Some("\"Blah\"")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal Maps" in {
        actualBody = Some("{\"something\": 100}")
        expectedBody = Some("{\"something\": 100}")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal Lists" in {
        actualBody = Some("[100,200,300]")
        expectedBody = Some("[100, 200, 300]")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

    "returns a mismatch" should {

      def containMessage(s: String) = (a: List[BodyMismatch]) => (
          a.exists((m: BodyMismatch) => m.mismatch.get == s),
          s"$a does not contain '$s'"
        )

      "when comparing anything to an empty body" in {
        expectedBody = Some("Blah")
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when comparing an empty map to a non-empty one" in {
        expectedBody = Some("{}")
        actualBody = Some("{\"something\": 100}")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty Map but received Map(something -> 100)")
      }

      "when comparing an empty list to a non-empty one" in {
        expectedBody = Some("[]")
        actualBody = Some("[100]")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty List but received List(100)")
      }

      "when comparing a map to one with less entries" in {
        expectedBody = Some("{\"something\": 100, \"somethingElse\": 100}")
        actualBody = Some("{\"something\": 100}")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected a Map with at least 2 elements but received 1 elements")
      }

      "when comparing a list to one with with different size" in {
        expectedBody = Some("[1,2,3,4]")
        actualBody = Some("[1,2,3]")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must have size 2
        mismatches must containMessage("Expected a List with 4 elements but received 3 elements")
        mismatches must containMessage("Expected 4 but was missing")
      }

      "when the actual body is missing a key" in {
        expectedBody = Some("{\"something\": 100, \"somethingElse\": 100}")
        actualBody = Some("{\"something\": 100}")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse=100 but was missing")
      }

      "when the actual body has invalid value" in {
        expectedBody = Some("{\"something\": 100}")
        actualBody = Some("{\"something\": 101}")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected 100 but received 101")
      }

      "when comparing a map to a list" in {
        expectedBody = Some("{\"something\": 100, \"somethingElse\": 100}")
        actualBody = Some("[100, 100]")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Type mismatch: Expected JObject JObject(List((something,JInt(100)), (somethingElse,JInt(100)))) but received JArray JArray(List(JInt(100), JInt(100)))")
      }

      "when comparing list to anything" in {
        expectedBody = Some("[100, 100]")
        actualBody = Some("100")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Type mismatch: Expected JArray JArray(List(JInt(100), JInt(100))) but received JInt JInt(100)")
      }

    }

    "with a matcher defined" should {

      "delegate to the matcher" in {
        expectedBody = Some("{\"something\": 100}")
        actualBody = Some("{\"something\": 101}")
        matchers = Some(Map("$.body.something" -> Map("regex" -> "\\d+")))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

  }

}
