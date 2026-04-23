// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 11
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.HttpRequestBuilder
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.core.model.HttpRequest
import org.junit.jupiter.api.Test

class README_kotlin_block11_Test {

    private fun withRequest(block: HttpRequestBuilder.() -> Unit) {
        HttpRequestBuilder(HttpRequest()).apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:11
        withRequest {
            method("POST")
            path("/api/orders")
            body(newJsonObject {
                stringType("productId", "abc-123")
                numberType("quantity", 1)
            })
        }
        // @DOCTEST-END
    }
}
