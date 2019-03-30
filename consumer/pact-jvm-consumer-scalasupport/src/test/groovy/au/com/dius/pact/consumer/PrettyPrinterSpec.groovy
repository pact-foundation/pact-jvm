package au.com.dius.pact.consumer

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.matchers.PartialRequestMatch
import au.com.dius.pact.core.matchers.PathMismatch
import au.com.dius.pact.core.model.OptionalBody
import au.com.dius.pact.core.model.ProviderState
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import spock.lang.Specification

class PrettyPrinterSpec extends Specification {

    def headers = [testreqheader: 'testreqheadervalue', 'Content-Type': 'application/json']
    def request = new Request('POST', '/', null, headers, OptionalBody.body('{"test": true}'.bytes))
    def response = new Response(200, [testreqheader: 'testreqheaderval', 'Access-Control-Allow-Origin': '*'],
      OptionalBody.body('{"responsetest": true}'.bytes))

    def print(mismatch) {
      PrettyPrinter.print(PactSessionResults.empty().addAlmostMatched(
        new PartialRequestMatch([(new RequestResponseInteraction('test interaction', [
          new ProviderState('test state')], request, response)): [mismatch]])))
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
        print(new PathMismatch('/foo/bar', '/foo/baz')) ==
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
