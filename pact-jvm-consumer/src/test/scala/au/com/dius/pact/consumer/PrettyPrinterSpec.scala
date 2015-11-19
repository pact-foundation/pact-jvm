package au.com.dius.pact.consumer

import au.com.dius.pact.consumer.Fixtures._
import au.com.dius.pact.model.{BodyMismatch, HeaderMismatch, PathMismatch, _}
import org.json4s.jackson.JsonMethods._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PrettyPrinterSpec extends Specification {
  def print(mismatch: RequestPartMismatch) = {
    PrettyPrinter.print(PactSessionResults.empty.addAlmostMatched(PartialRequestMatch(interaction, Seq(mismatch))))
  }

  def plus = "+++ "

  "header mismatch" in {
    print(HeaderMismatch("foo", "bar", "", None)) must beEqualTo(
      s"""--- Header foo
        |$plus
        |@@ -1,1 +1,1 @@
        |-bar
        |+""".stripMargin
    )
  }

  "path mismatch" in {
    print(PathMismatch("/foo/bar", "/foo/baz")) must beEqualTo(
    s"""--- Path
      |$plus
      |@@ -1,1 +1,1 @@
      |-/foo/bar
      |+/foo/baz""".stripMargin
    )
  }

  "body mismatch" in {
    import org.json4s.JsonDSL._

    print(BodyMismatch(pretty(map2jvalue(Map("foo"->"bar"))), pretty(map2jvalue(Map("ork" -> "Bif"))))) must beEqualTo(
    s"""--- Body
      |$plus
      |@@ -1,3 +1,3 @@
      | {
      |-  "foo" : "bar"
      |+  "ork" : "Bif"
      | }""".stripMargin
    )
  }
}
