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

    "content type header" in {

      "be true when headers are equal" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json;charset=UTF-8", "application/json; charset=UTF-8", None) must beNone
      }

      "be false when headers are not equal" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json;charset=UTF-8", "application/pdf;charset=UTF-8", None) must not beNone
      }

      "be false when charsets are not equal" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json;charset=UTF-8", "application/json;charset=UTF-16", None) must not beNone
      }

      "be false when other parameters are not equal" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json;declaration=\"<950118.AEB0@XIson.com>\"", "application/json;charset=UTF-8", None) must not beNone
      }

      "be true when the charset is missing from the expected header" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json", "application/json ; charset=UTF-8", None) must beNone
      }

      "delegate to any defined matcher" in {
        HeaderMatcher.compareHeader("CONTENT-TYPE", "application/json", "application/json;charset=UTF-8", Some(Map("$.headers.CONTENT-TYPE" -> Map("regex" -> "[a-z]+\\/[a-z]+")))) must not beNone
      }

    }
  }

  "parse parameters" should {

    "parse the parameters into a map" in {
      HeaderMatcher.parseParameters(Array("A=B")) must beEqualTo(Map("A" -> "B"))
      HeaderMatcher.parseParameters(Array("A=B", "C=D")) must beEqualTo(Map("A" -> "B", "C" -> "D"))
      HeaderMatcher.parseParameters(Array("A= B", "C =D ")) must beEqualTo(Map("A" -> "B", "C" -> "D"))
    }

  }

}
