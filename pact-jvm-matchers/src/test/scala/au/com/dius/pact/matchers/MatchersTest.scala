package au.com.dius.pact.matchers

import au.com.dius.pact.model.Request
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import au.com.dius.pact.model.JsonDiff.DiffConfig

@RunWith(classOf[JUnitRunner])
class MatchersTest extends Specification {

  "matchers defined" should {

    "should be false when there are no matchers" in {
      Matchers.matcherDefined("", None) must beFalse
    }

    "should be false when the path does not have a matcher entry" in {
      Matchers.matcherDefined("$.body.something", Some(Map())) must beFalse
    }

    "should be true when the path does have a matcher entry" in {
      Matchers.matcherDefined("$.body.something", Some(Map("$.body.something" -> None))) must beTrue
    }

  }

  "regex matcher" should {

    "match using the provided regex" in {
      val expected = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), Some(Map("$.body.value" -> Map("regex" -> "Ha[a-z]*"))))
      val actual = Request("get", "/", None, None, Some("{\"value\": \"Harry\"}"), None)
      new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must beEmpty
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
        val expected = Request("get", "/", None, None, Some("{\"value\": null}"), Some(Map("$.body.value" -> Map("match" -> "type"))))
        val actual = Request("get", "/", None, None, Some("{\"value\": 200}"), None)
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

    }

  }

}
