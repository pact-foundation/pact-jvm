package au.com.dius.pact.model

import org.specs2.mutable.Specification
import Fixtures._

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MatchingSpec extends Specification {
  "Matching" should {
    import Matching._
    import JsonDiff._
    implicit val autoParse = JsonDiff.autoParse _
    "Body Matching" should {
      val config = DiffConfig()

      "Handle both None" in {
        matchBody("a", None, "a", None, config) must beEmpty
      }

      "Handle left None" in {
        val expected = List(BodyMismatch(request.body, None))
        matchBody("a", request.body, "a", None, config) must beEqualTo(expected)
      }

      "Handle right None" in {
        val expected = List(BodyMismatch(None, request.body))
        matchBody("a", None, "a", request.body, config) must beEqualTo(expected)
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
  }
}
