package au.com.dius.pact.consumer

import au.com.dius.pact.matchers.BodyMismatch
import au.com.dius.pact.matchers.HeaderMismatch
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.model.PartialRequestMatch
import au.com.dius.pact.model.PathMismatch
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.model.RequestPartMismatch
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import scala.Option
import scala.collection.JavaConversions
import scala.collection.Seq
import spock.lang.Specification

class PrettyPrinterSpec extends Specification {

    def headers = [testreqheader: 'testreqheadervalue', 'Content-Type': 'application/json']
    def request = new Request('POST', '/', null, headers, OptionalBody.body('{"test": true}'))
    def response = new Response(200, [testreqheader: 'testreqheaderval', 'Access-Control-Allow-Origin': '*'],
      OptionalBody.body('{"responsetest": true}'))

    def print(mismatch) {
      PrettyPrinter.print(PactSessionResults.empty().addAlmostMatched(
        PartialRequestMatch.apply(new RequestResponseInteraction('test interaction', [
          new ProviderState('test state')], request, response),
          JavaConversions.asScalaBuffer([mismatch]).toSeq() as Seq<RequestPartMismatch>)))
    }

    def plus = '+++ '

    def 'header mismatch'() {
        expect:
        print(new HeaderMismatch('foo', 'bar', '', null)) ==
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
        print(new BodyMismatch('{"foo": "bar"}', '{"ork": "Bif"}')) ==
              """--- Body
              |$plus
              |@@ -1,3 +1,3 @@
              | {
              |-    "foo": "bar"
              |+    "ork": "Bif"
              | }""".stripMargin()
    }
}
