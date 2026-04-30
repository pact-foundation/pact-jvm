// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 22
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.consumer.dsl.Matchers.regexp
import au.com.dius.pact.consumer.dsl.PM
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block22_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:22
        withRequest {
            method("GET")
            path(regexp("\\/orders\\/[0-9]+", "/orders/42"))
            header("X-Trace-Id", PM.uuid())
            queryParameter("status", PM.stringMatcher("active|pending", "active"))
        }
        // @DOCTEST-END
    }
}
