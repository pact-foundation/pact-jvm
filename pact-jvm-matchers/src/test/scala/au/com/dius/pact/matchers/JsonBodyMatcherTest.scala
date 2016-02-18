package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, CollectionUtils, DiffConfig, Request}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class JsonBodyMatcherTest extends Specification with AllExpectations {
  isolated

  var expectedBody: String = null
  var actualBody: String = null
  var matchers = Map[String, Map[String, String]]()
  val expected = () => new Request("", "", null, null, expectedBody, CollectionUtils.scalaMMapToJavaMMap(matchers))
  val actual = () => new Request("", "", null, null, actualBody)

  var diffconfig = DiffConfig(structural = true)

  "matching json bodies" should {

    val matcher = new JsonBodyMatcher()

    "return no mismatches" should {

      "when comparing empty bodies" in {
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when comparing an empty body to anything" in {
        actualBody = "Blah"
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal bodies" in {
        actualBody = "\"Blah\""
        expectedBody = "\"Blah\""
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal Maps" in {
        actualBody = "{\"something\": 100}"
        expectedBody = "{\"something\": 100}"
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal Lists" in {
        actualBody = "[100,200,300]"
        expectedBody = "[100, 200, 300]"
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with each like matcher on unequal lists" in {
        actualBody = "{\"list\": [100, 200, 300, 400]}"
        expectedBody = "{\"list\": [100]}"
        matchers = Map("$.body.list" -> Map("min" -> "1","match" -> "type"))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with each like matcher on empty list" in {
        actualBody = "{\"list\": []}"
        expectedBody = "{\"list\": [100]}"
        matchers = Map("$.body.list" -> Map("min" -> "0","match" -> "type"))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

    "returns a mismatch" should {

      def containMessage(s: String) = (a: List[BodyMismatch]) => (
          a.exists((m: BodyMismatch) => m.mismatch.get == s),
          s"$a does not contain '$s'"
        )

      "when comparing anything to an null body" in {
        expectedBody = "Blah"
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when comparing anything to an empty body" in {
        expectedBody = "Blah"
        actualBody = ""
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when comparing an empty map to a non-empty one" in {
        expectedBody = "{}"
        actualBody = "{\"something\": 100}"
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty Map but received Map(something -> 100)")
      }

      "when comparing an empty list to a non-empty one" in {
        expectedBody = "[]"
        actualBody = "[100]"
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty List but received List(100)")
      }

      "when comparing a map to one with less entries" in {
        expectedBody = "{\"something\": 100, \"somethingElse\": 100}"
        actualBody = "{\"something\": 100}"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected a Map with at least 2 elements but received 1 elements")
      }

      "when comparing a list to one with with different size" in {
        expectedBody = "[1,2,3,4]"
        actualBody = "[1,2,3]"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must have size 2
        mismatches must containMessage("Expected a List with 4 elements but received 3 elements")
        mismatches must containMessage("Expected 4 but was missing")
      }

      "when the actual body is missing a key" in {
        expectedBody = "{\"something\": 100, \"somethingElse\": 100}"
        actualBody = "{\"something\": 100}"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse=100 but was missing")
      }

      "when the actual body has invalid value" in {
        expectedBody = "{\"something\": 100}"
        actualBody = "{\"something\": 101}"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected 100 but received 101")
      }

      "when comparing a map to a list" in {
        expectedBody = "{\"something\": 100, \"somethingElse\": 100}"
        actualBody = "[100, 100]"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Type mismatch: Expected Map Map(something -> 100, somethingElse -> 100) but received List List(100, 100)")
      }

      "when comparing list to anything" in {
        expectedBody = "[100, 100]"
        actualBody = "100"
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Type mismatch: Expected List List(100, 100) but received Integer 100")
      }

    }

    "with a matcher defined" should {

      "delegate to the matcher" in {
        expectedBody = "{\"something\": 100}"
        actualBody = "{\"something\": 101}"
        matchers = Map("$.body.something" -> Map("regex" -> "\\d+"))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

  }

}
