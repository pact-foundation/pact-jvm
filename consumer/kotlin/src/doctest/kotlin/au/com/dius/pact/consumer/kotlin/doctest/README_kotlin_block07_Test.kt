// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 7
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.consumer.dsl.Matchers.regexp
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block07_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:7
        withRequest {
            method("GET")
            path("/api/data")
            header("Accept", "application/json")
            header("Authorization", regexp("Bearer .+", "Bearer token123"))
        }
        // @DOCTEST-END
    }
}
