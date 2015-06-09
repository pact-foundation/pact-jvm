package au.com.dius.pact.matchers

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HeaderMatcherSpec extends Specification {

  "matching headers" should {
    "be true when headers are equal" in {
      HeaderMatcher.compareHeader("HEADER", "HEADER", "HEADER", None) must beNone
    }

    "be false when headers are not equal" in {
      HeaderMatcher.compareHeader("HEADER", "HEADER", "HEADSER", None) must not beNone
    }

    "exclude whitespace from the comparison" in {
      HeaderMatcher.compareHeader("HEADER", "HEADER1, HEADER2,   3", "HEADER1,HEADER2,3", None) must beNone
    }

    "delegate to a matcher when one is defined" in {
      HeaderMatcher.compareHeader("HEADER", "HEADER", "XYZ", Some(Map("$.headers.HEADER" -> Map("regex" -> ".*")))) must beNone
    }
  }

}
