package au.com.dius.pact.matchers

import au.com.dius.pact.model.{BodyMismatchFactory, BodyMismatch, Request}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import au.com.dius.pact.model.DiffConfig

@RunWith(classOf[JUnitRunner])
class MatchersTest extends Specification {

  "matchers defined" should {

    "should be false when there are no matchers" in {
      Matchers.matcherDefined(Seq(""), None) must beFalse
    }

    "should be false when the path does not have a matcher entry" in {
      Matchers.matcherDefined(Seq("$", "body", "something"), Some(Map())) must beFalse
    }

    "should be true when the path does have a matcher entry" in {
      Matchers.matcherDefined(Seq("$", "body", "something"), Some(Map("$.body.something" -> Map[String, String]()))) must beTrue
    }

  }

  "should default to equality matching if the matcher is unknown" in {
    Matchers.matcher(Map("other" -> "something")) must be(EqualsMatcher)
    Matchers.matcher(Map()) must be(EqualsMatcher)
  }

  "equal matcher" should {

    "match using equals" in {
      EqualsMatcher.domatch[BodyMismatch](null, Seq("/"), "100", "100", BodyMismatchFactory).isEmpty must beTrue
      EqualsMatcher.domatch[BodyMismatch](null, Seq("/"), 100, "100", BodyMismatchFactory).isEmpty must beFalse
    }

  }

  "regex matcher" should {

    "match using the provided regex" in {
      val expected = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), Some(Map("$.body.value" -> Map("regex" -> "Ha[a-z]*"))))
      val actual = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), None)
      new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
    }

    "handle null values" in {
      val expected = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), Some(Map("$.body.value" -> Map("regex" -> "Ha[a-z]*"))))
      val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
      new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
    }

  }

  "type matcher" should {

    "match on type" should {

      "accept strings" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"Some other string\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept numbers" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": 100}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": 200.3}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept booleans" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": true}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": false}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "accept null" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": null}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "not accept different types" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"200\"}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": 200}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "not accept null/non-null" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": 200}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match timestamps" should {

      "accept ISO formatted timestamps" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"2014-01-01 14:00:00+10:00\"}"), Some(Map("$.body.value" -> Map("match" -> "timestamp"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"2014-10-01 14:00:00+10:00\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "not accept incorrect formatted timestamps" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"2014-01-01 14:00:00\"}"), Some(Map("$.body.value" -> Map("match" -> "timestamp"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"I'm a timestamp!\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept timestamps with custom patterns" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"2014-01-01-14:00:00+10:00\"}"), Some(Map("$.body.value" -> Map("timestamp" -> "yyyy-MM-dd-HH:mm:ssZZZ"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"2014-10-01-14:00:00+10:00\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"2014-01-01-14:00:00+10:00\"}"), Some(Map("$.body.value" -> Map("timestamp" -> "yyyy-MM-dd-HH:mm:ssZZZ"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match times" should {

      "not accept incorrect formatted times" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"00:00\"}"), Some(Map("$.body.value" -> Map("time" -> "mm:ss"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"14:01:02\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept times with custom patterns" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"00:00:14\"}"), Some(Map("$.body.value" -> Map("time" -> "ss:mm:HH"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"05:10:14\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"14:00:00\"}"), Some(Map("$.body.value" -> Map("time" -> "HH:mm:ss"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

    "match dates" should {

      "not accept incorrect formatted dates" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"01-01-1970\"}"), Some(Map("$.body.value" -> Map("date" -> "dd-MM-yyyy"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"01011970\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "accept dates with custom patterns" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"12/30/1970\"}"), Some(Map("$.body.value" -> Map("date" -> "MM/dd/yyyy"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": \"12/30/1970\"}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
      }

      "handle null values" in {
        val expected = Request("get", "/", None, None, Some("{\"value\": \"2014-01-01\"}"), Some(Map("$.body.value" -> Map("date" -> "yyyy-MM-dd"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": null}"), None)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

    }

  }

  "path matching" should {

    "match root node" in {
      Matchers.matchesPath("$", Seq("$")) must beTrue
      Matchers.matchesPath("$", Seq()) must beFalse
    }

    "match field name" in {
      Matchers.matchesPath("$.name", Seq("$", "name")) must beTrue
      Matchers.matchesPath("$.name.other", Seq("$", "name", "other")) must beTrue
      Matchers.matchesPath("$.name", Seq("$", "other")) must beFalse
      Matchers.matchesPath("$.name", Seq("$", "name", "other")) must beFalse
    }

    "match array indices" in {
      Matchers.matchesPath("$[0]", Seq("$", "0")) must beTrue
      Matchers.matchesPath("$.name[1]", Seq("$", "name", "1")) must beTrue
      Matchers.matchesPath("$.name", Seq("$", "0")) must beFalse
      Matchers.matchesPath("$.name[1]", Seq("$", "name", "0")) must beFalse
      Matchers.matchesPath("$[1].name", Seq("$", "name", "1")) must beFalse
    }

    "match with wildcard" in {
      Matchers.matchesPath("$[*]", Seq("$", "0")) must beTrue
      Matchers.matchesPath("$.*", Seq("$", "name")) must beTrue
      Matchers.matchesPath("$.*.name", Seq("$", "some", "name")) must beTrue
      Matchers.matchesPath("$.name[*]", Seq("$", "name", "0")) must beTrue
      Matchers.matchesPath("$.name[*].name", Seq("$", "name", "1", "name")) must beTrue

      Matchers.matchesPath("$[*]", Seq("$", "str")) must beFalse
    }

  }

}
