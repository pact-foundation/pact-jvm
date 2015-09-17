package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatch, BodyMismatchFactory}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MinimumMatcherTest extends Specification {

  val matcherDef = Map("min" -> 2, "match" -> "type")

  "with an array" should {

    val path: Seq[String] = Seq("$", "body", "animals", "0", "children")

    "match if the array is larger" in {
       MinimumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1, 2, 3), BodyMismatchFactory) must beEmpty
     }

     "match if the array is the correct size" in {
       MinimumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1, 3), BodyMismatchFactory) must beEmpty
     }

     "not match if the array is smaller" in {
       MinimumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1), BodyMismatchFactory) must not(beEmpty)
     }

  }

  "with a non-array" should {

    val path = Seq("$", "body", "animals", "0", "children", "0")

    "default to type matcher" in {
      MinimumMatcher.domatch[BodyMismatch](matcherDef, path, "Fred", "George", BodyMismatchFactory) must beEmpty
      MinimumMatcher.domatch[BodyMismatch](matcherDef, path, "Fred", 100, BodyMismatchFactory) must not(beEmpty)
    }

  }

}
