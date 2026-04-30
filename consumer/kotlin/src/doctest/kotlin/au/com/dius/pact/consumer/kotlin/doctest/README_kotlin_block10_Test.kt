// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 10
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block10_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:10
        withRequest {
            method("POST")
            path("/api/orders")
            body("""{"productId": "abc-123", "quantity": 2}""", "application/json")
        }
        // @DOCTEST-END
    }
}
