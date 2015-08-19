package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, DiffConfig, Request}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.AllExpectations

@RunWith(classOf[JUnitRunner])
class XmlBodyMatcherTest extends Specification with AllExpectations {
  isolated

  var expectedBody: Option[String] = None
  var actualBody: Option[String] = None
  var matchers: Option[Map[String, Map[String, String]]] = None
  val expected = () => Request("", "", None, None, expectedBody, matchers)
  val actual = () => Request("", "", None, None, actualBody, None)

  var diffconfig = DiffConfig(structural = true, allowUnexpectedKeys = false)

  "matching XML bodies" should {

    val matcher = new XmlBodyMatcher()

    "return no mismatches" should {

      "when comparing empty bodies" in {
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when comparing an empty body to anything" in {
        actualBody = Some("Blah")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "with equal bodies" in {
        actualBody = Some(<blah/>.toString())
        expectedBody = Some(<blah/>.toString())
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when bodies differ only in whitespace" in {
        actualBody = Some(
          """<foo>
            |  <bar></bar>
            |</foo>
          """.stripMargin)

        expectedBody = Some("<foo><bar></bar></foo>")
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

      "when allowUnexpectedKeys is true" in {

        val allowUnexpectedKeys = DiffConfig(structural = true, allowUnexpectedKeys = true)

        "and comparing an empty list to a non-empty one" in {
          expectedBody = Some("<foo></foo>")
          actualBody = Some("<foo><item/></foo>")
          matcher.matchBody(expected(), actual(), allowUnexpectedKeys) must beEmpty
        }

        "and comparing a list to a super-set" in {
          expectedBody = Some("<foo><item1/></foo>")
          actualBody = Some("<foo><item1/><item2/></foo>")
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
        expectedBody = Some(<blah/>.toString())
        matcher.matchBody(expected(), actual(), diffconfig) must not(beEmpty)
      }

      "when the root elements do not match" in {
        expectedBody = Some("<foo/>")
        actualBody = Some("<bar></bar>")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected element foo but received bar")
        mismatches must havePath("$.body")
      }

      "when comparing an empty list to a non-empty one" in {
        expectedBody = Some("<foo></foo>")
        actualBody = Some("<foo><item/></foo>")
        val mismatches: List[BodyMismatch] = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected an empty List but received <item/>")
        mismatches must havePath("$.body.foo")
      }

      "when comparing a list to one with with different size" in {
        expectedBody = Some("<foo><one/><two/><three/><four/></foo>")
        actualBody = Some("<foo><one/><two/><three/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must have size 2
        mismatches must containMessage("Expected a List with 4 elements but received 3 elements")
        mismatches must containMessage("Expected <four/> but was missing")
        mismatches must havePath("$.body.foo")
      }

      "when comparing a list to one with with the same size but different children" in {
        expectedBody = Some("<foo><one/><two/><three/></foo>")
        actualBody = Some("<foo><one/><two/><four/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)

        mismatches must containMessage("Expected element three but received four")
        mismatches must havePath("$.body.foo[2]")
      }

      "when comparing a list to one where the items are in the wrong order" in {
        expectedBody = Some("<foo><one/><two/><three/></foo>")
        actualBody = Some("<foo><one/><three/><two/></foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)

        mismatches must containMessage("Expected element two but received three")
        mismatches must containMessage("Expected element three but received two")
      }

      "when comparing a tags attributes to one with less entries" in {
        expectedBody = Some("<foo something=\"100\" somethingElse=\"101\"/>")
        actualBody = Some("<foo something=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected a Tag with at least 2 attributes but received 1 attributes")
      }

      "when a tag is missing an attribute" in {
        expectedBody = Some("<foo something=\"100\" somethingElse=\"100\"/>")
        actualBody = Some("<foo something=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse=100 but was missing")
      }

      "when a tag has the same number of attributes but different keys" in {
        expectedBody = Some("<foo something=\"100\" somethingElse=\"100\"/>")
        actualBody = Some("<foo something=\"100\" somethingDifferent=\"100\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected somethingElse=100 but was missing")
        mismatches must havePath("$.body.foo.somethingElse")
      }

      "when a tag has an invalid value" in {
        expectedBody = Some("<foo something=\"100\"/>")
        actualBody = Some("<foo something=\"101\"/>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected something=100 but received 101")
        mismatches must havePath("$.body.foo.something")
      }

      "when the content of an element does not match" in {
        expectedBody = Some("<foo>hello world</foo>")
        actualBody = Some("<foo>hello my friend</foo>")
        val mismatches = matcher.matchBody(expected(), actual(), diffconfig)
        mismatches must not(beEmpty)
        mismatches must containMessage("Expected value 'hello world' but received 'hello my friend'")
        mismatches must havePath("$.body.foo[0]")
      }
    }

    "with a matcher defined" should {

      "delegate to the matcher" in {
        expectedBody = Some("<foo something=\"100\"/>")
        actualBody = Some("<foo something=\"101\"/>")
        matchers = Some(Map("$.body.foo.something" -> Map("regex" -> "\\d+")))
        matcher.matchBody(expected(), actual(), diffconfig) must beEmpty
      }

    }

  }

}
