package au.com.dius.pact.matchers

import au.com.dius.pact.model.{DiffConfig, OptionalBody, Request}
import au.com.dius.pact.model.matchingrules.{MatchingRules, TypeMatcher}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.JavaConversions

@RunWith(classOf[JUnitRunner])
class MatchersTest extends Specification {

  "matchers defined" should {

    "should be false when there are no matchers" in {
      Matchers.matcherDefined("body", Seq(""), new MatchingRules()) must beFalse
    }

    "should be false when the path does not have a matcher entry" in {
      Matchers.matcherDefined("body", Seq("$", "something"), new MatchingRules()) must beFalse
    }

    "should be true when the path does have a matcher entry" in {
      val matchingRules = new MatchingRules()
      matchingRules.addCategory("body").addRule("$.something", new TypeMatcher())
      Matchers.matcherDefined("body", Seq("$", "something"), matchingRules) must beTrue
    }

    "should be true when a parent of the path has a matcher entry" in {
      val matchingRules = new MatchingRules()
      matchingRules.addCategory("body").addRule("$", new TypeMatcher())
      Matchers.matcherDefined("body", Seq("$", "something"), matchingRules) must beTrue
    }

  }

  "should default to a matching defined at a parent level" in {
    val matchingRules = new MatchingRules()
    matchingRules.addCategory("body").addRule("$", new TypeMatcher())
    val rules = Matchers.selectBestMatcher(matchingRules, "body", Seq("$", "value")).getMatchingRules
    JavaConversions.asScalaSet(rules.keySet()) must beEqualTo(Set("$"))
  }

  "type matcher" should {

    "match on type" should {

      "list elements should inherit the matcher from the parent" in {
        val matchingRules = new MatchingRules()
        matchingRules.addCategory("body").addRule("$.value", new TypeMatcher())
        val expected = new Request("get", "/", null, null, OptionalBody.body("{\"value\": [100]}"), matchingRules)
        val actual = new Request("get", "/", null, null, OptionalBody.body("{\"value\": [\"200.3\"]}"), null)
        new JsonBodyMatcher().matchBody(expected, actual, DiffConfig()) must not(beEmpty)
      }

      "map elements should inherit the matchers from the parent" in {
        val matchingRules = new MatchingRules()
        matchingRules.addCategory("body").addRule("$.value", new TypeMatcher())
        val expected = new Request("get", "/", null, null, OptionalBody.body("{\"value\": {\"a\": 100}}"), matchingRules)
        val actual = new Request("get", "/", null, null,
          OptionalBody.body("{\"value\": {\"a\": \"200.3\", \"b\": 200, \"c\": 300} }"), null)
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
      Matchers.matchesPath("$.name", Seq("$", "name", "other")) must beTrue
      Matchers.matchesPath("$.other", Seq("$", "name", "other")) must beFalse
      Matchers.matchesPath("$.name.other", Seq("$", "name")) must beFalse
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
