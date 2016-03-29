package au.com.dius.pact.consumer

import au.com.dius.pact.model.BodyMismatch
import au.com.dius.pact.model.HeaderMismatch
import au.com.dius.pact.model.PartialRequestMatch
import au.com.dius.pact.model.PathMismatch
import au.com.dius.pact.model.RequestPartMismatch
import scala.Option
import scala.collection.JavaConversions
import scala.collection.Seq
import spock.lang.Specification

class PrettyPrinterSpec extends Specification {
    def print(mismatch) {
        PrettyPrinter.print(PactSessionResults.empty().addAlmostMatched(
                PartialRequestMatch.apply(Fixtures.interaction(),
                        JavaConversions.asScalaBuffer([mismatch]).toSeq() as Seq<RequestPartMismatch>)))
    }

    def plus = '+++ '

    def 'header mismatch'() {
        expect:
        print(new HeaderMismatch('foo', 'bar', '', Option.empty())) ==
                """--- Header foo
                |$plus
                |@@ -1,1 +1,1 @@
                |-bar
                |+""".stripMargin()
    }

    def 'path mismatch'() {
        expect:
        print(new PathMismatch('/foo/bar', '/foo/baz', Option.empty())) ==
              """--- Path
              |$plus
              |@@ -1,1 +1,1 @@
              |-/foo/bar
              |+/foo/baz""".stripMargin()
    }

    def 'body mismatch'() {
        expect:
        print(new BodyMismatch('{"foo": "bar"}', '{"ork": "Bif"}', Option.empty(), '/')) ==
              """--- Body
              |$plus
              |@@ -1,3 +1,3 @@
              | {
              |-    "foo": "bar"
              |+    "ork": "Bif"
              | }""".stripMargin()
    }
}
