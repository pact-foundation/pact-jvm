// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 9
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block09_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:9
        withRequest {
            method("POST")
            path("/api/echo")
            body("hello")
        }
        // @DOCTEST-END
    }
}
