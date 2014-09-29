package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, JsonDiff, Request}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class JsonBodyMatcherTest extends Specification with AllExpectations {
  isolated

  var expectedBody: Option[String] = None
  var actualBody: Option[String] = None
  val expected = () => Request("", "", None, None, expectedBody, None)
  val actual = () => Request("", "", None, None, actualBody, None)

  var diffconfig = JsonDiff.DiffConfig(structural = true)

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

    }

    "returns a mismatch" should {

      def containMessage(a: List[BodyMismatch], s: String) = new Matcher[List[BodyMismatch]] {}

      "when comparing an empty body to anything and we do not allow extra keys" in {
        actualBody = Some("Blah")
        matcher.matchBody(expected(), actual(), JsonDiff.DiffConfig(structural = false)) must not(beEmpty)
      }

      "when comparing anything to an empty body" in {
        expectedBody = Some("Blah")
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when comparing an empty map to a non-empty one" in {
        expectedBody = Some("{}")
        actualBody = Some("{\"something\": 100}")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must contain((mismatch: BodyMismatch) => mismatch.mismatch must beSome("Expected an empty Map but received JObject(List((something,JInt(100))))"))
      }

      "when comparing a map to one with less entries" in {
        expectedBody = Some("{\"something\": 100, \"somethingElse\": 100}")
        actualBody = Some("{\"something\": 100}")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must contain((mismatch: BodyMismatch) => mismatch.mismatch must beSome("Expected a Map with at least 2 elements but received 1 elements"))
      }

      "when the actual body is missing a key" in {
        expectedBody = Some("{\"something\": 100, \"somethingElse\": 100}")
        actualBody = Some("{\"something\": 100}")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must contain((mismatch: BodyMismatch) => mismatch.mismatch must beSome("Expected somethingElse=100 but was missing"))
      }

    }

  }

}
