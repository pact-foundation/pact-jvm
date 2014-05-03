package au.com.dius.pact.model

import org.specs2.mutable.Specification
import Fixtures._

class MatchingSpec extends Specification {
  "Matching" should {
    import Matching._
    import JsonDiff._
    implicit val autoParse = JsonDiff.autoParse _
    "Body Matching" should {
      val config = DiffConfig()
      "Handle both None" in {
        matchBody(None, None, config) must beNone
      }
      "Handle left None" in {
        val expected = BodyMismatch(missing(request.body.get))
        matchBody(request.body, None, config) must beSome(expected)
      }
      "Handle right None" in {
        val expected = BodyMismatch(added(request.body.get))
        matchBody(None, request.body, config) must beSome(expected)
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
