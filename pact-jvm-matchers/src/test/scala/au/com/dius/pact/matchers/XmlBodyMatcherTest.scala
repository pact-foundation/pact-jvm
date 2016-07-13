package au.com.dius.pact.matchers

import au.com.dius.pact.model._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class XmlBodyMatcherTest extends Specification with AllExpectations {
  isolated

  var expectedBody = OptionalBody.missing()
  var actualBody = OptionalBody.missing()
  var matchers = Map[String, Map[String, String]]()
  val expected = () => new Request("", "", null, null, expectedBody,
    CollectionUtils.scalaMMapToJavaMMap(matchers))
  val actual = () => new Request("", "", null, null, actualBody)

  var diffconfig = DiffConfig(structural = true, allowUnexpectedKeys = false)

  "matching XML bodies" should {

    val matcher = new XmlBodyMatcher()

    "return no mismatches" should {

      "when comparing missing bodies" in {
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when comparing empty bodies" in {
        actualBody = OptionalBody.empty()
        expectedBody = OptionalBody.empty()
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when comparing a missing body to anything" in {
        actualBody = OptionalBody.body("Blah")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal bodies" in {
        actualBody = OptionalBody.body(<blah/>.toString())
        expectedBody = OptionalBody.body(<blah/>.toString())
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when bodies differ only in whitespace" in {
        actualBody = OptionalBody.body(
          """<foo>
            |  <bar></bar>
            |</foo>
          """.stripMargin)

        expectedBody = OptionalBody.body("<foo><bar></bar></foo>")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when allowUnexpectedKeys is true" in {

        val allowUnexpectedKeys = DiffConfig(structural = true, allowUnexpectedKeys = true)

        "and comparing an empty list to a non-empty one" in {
          expectedBody = OptionalBody.body("<foo></foo>")
          actualBody = OptionalBody.body("<foo><item/></foo>")
          matcher.matchBody(expected(), actual(), allowUnexpectedKeys) must beEmpty
        }

        "and comparing a list to a super-set" in {
          expectedBody = OptionalBody.body("<foo><item1/></foo>")
          actualBody = OptionalBody.body("<foo><item1/><item2/></foo>")
          matcher.matchBody(expected(), actual(), allowUnexpectedKeys) must beEmpty
        }

        "and comparing a tags attributes to one with more entries" in {
          expectedBody = OptionalBody.body("<foo something=\"100\"/>")
          actualBody = OptionalBody.body("<foo something=\"100\" somethingElse=\"101\"/>")
          matcher.matchBody(expected(), actual(), allowUnexpectedKeys) must beEmpty
        }

      }

    }

    "returns a mismatch" should {

      def containMessage(s: String) = (a: List[BodyMismatch]) => (
          a.exists((m: BodyMismatch) => m.mismatch.get == s),
          s"$a does not contain '$s'"
        )

      def havePath(p: String) = (a: List[BodyMismatch]) => (
        a.forall((m: BodyMismatch) => m.path == p),
        s"$a does not have path '$p', paths are: ${a.map(m => m.path).mkString(",")}"
        )


      "when comparing anything to an empty body" in {
        expectedBody = OptionalBody.body(<blah/>.toString())
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when the root elements do not match" in {
        expectedBody = OptionalBody.body("<foo/>")
        actualBody = OptionalBody.body("<bar></bar>")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected element foo but received bar")
        mismatches must havePath("$.body.foo")
      }

      "when comparing an empty list to a non-empty one" in {
        expectedBody = OptionalBody.body("<foo></foo>")
        actualBody = OptionalBody.body("<foo><item/></foo>")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty List but received <item/>")
        mismatches must havePath("$.body.foo")
      }

      "when comparing a list to one with with different size" in {
        expectedBody = OptionalBody.body("<foo><one/><two/><three/><four/></foo>")
        actualBody = OptionalBody.body("<foo><one/><two/><three/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must have size 2
        mismatches must containMessage("Expected a List with 4 elements but received 3 elements")
        mismatches must containMessage("Expected <four/> but was missing")
        mismatches must havePath("$.body.foo")
      }

      "when comparing a list to one with with the same size but different children" in {
        expectedBody = OptionalBody.body("<foo><one/><two/><three/></foo>")
        actualBody = OptionalBody.body("<foo><one/><two/><four/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)

        mismatches must containMessage("Expected element three but received four")
        mismatches must havePath("$.body.foo.2.three")
      }

      "when comparing a list to one where the items are in the wrong order" in {
        expectedBody = OptionalBody.body("<foo><one/><two/><three/></foo>")
        actualBody = OptionalBody.body("<foo><one/><three/><two/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)

        mismatches must containMessage("Expected element two but received three")
        mismatches must containMessage("Expected element three but received two")
      }

      "when comparing a tags attributes to one with less entries" in {
        expectedBody = OptionalBody.body("<foo something=\"100\" somethingElse=\"101\"/>")
        actualBody = OptionalBody.body("<foo something=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected a Tag with 2 attributes but received 1 attributes")
      }

      "when comparing a tags attributes to one with more entries" in {
        expectedBody = OptionalBody.body("<foo something=\"100\"/>")
        actualBody = OptionalBody.body("<foo something=\"100\" somethingElse=\"101\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected a Tag with 1 attributes but received 2 attributes")
      }

      "when a tag is missing an attribute" in {
        expectedBody = OptionalBody.body("<foo something=\"100\" somethingElse=\"100\"/>")
        actualBody = OptionalBody.body("<foo something=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse='100' but was missing")
      }

      "when a tag has the same number of attributes but different keys" in {
        expectedBody = OptionalBody.body("<foo something=\"100\" somethingElse=\"100\"/>")
        actualBody = OptionalBody.body("<foo something=\"100\" somethingDifferent=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse='100' but was missing")
        mismatches must havePath("$.body.foo.@somethingElse")
      }

      "when a tag has an invalid value" in {
        expectedBody = OptionalBody.body("<foo something=\"100\"/>")
        actualBody = OptionalBody.body("<foo something=\"101\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected something='100' but received 101")
        mismatches must havePath("$.body.foo.@something")
      }

      "when the content of an element does not match" in {
        expectedBody = OptionalBody.body("<foo>hello world</foo>")
        actualBody = OptionalBody.body("<foo>hello my friend</foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected value 'hello world' but received 'hello my friend'")
        mismatches must havePath("$.body.foo.#text")
      }
    }

    "with a matcher defined" should {

      "delegate to the matcher" in {
        expectedBody = OptionalBody.body("<foo something=\"100\"/>")
        actualBody = OptionalBody.body("<foo something=\"101\"/>")
        matchers = Map("$.body.foo['@something']" -> Map("regex" -> "\\d+"))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

  }

}
