package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatchFactory, BodyMismatch}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MaximumMatcherTest extends Specification {

  private val matcherDef = Map("max" -> 2, "match" -> "type")

  "with an array" should {

    val path = Seq("$", "body", "animals", "0", "children")

    "match if the array is smaller" in {
      MaximumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1), BodyMismatchFactory) must beEmpty
    }

    "match if the array is the correct size" in {
      MaximumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1, 3), BodyMismatchFactory) must beEmpty
    }

    "not match if the array is larger" in {
      MaximumMatcher.domatch[BodyMismatch](matcherDef, path, List(1, 2), List(1, 2, 3), BodyMismatchFactory) must not(beEmpty)
    }

  }

  "with a non-array" should {

    val path = Seq("$", "body", "animals", "0", "children", "0")

    "default to type matcher" in {
      MaximumMatcher.domatch[BodyMismatch](matcherDef, path, "Fred", "George", BodyMismatchFactory) must beEmpty
      MaximumMatcher.domatch[BodyMismatch](matcherDef, path, "Fred", 100, BodyMismatchFactory) must not(beEmpty)
    }

  }

}
