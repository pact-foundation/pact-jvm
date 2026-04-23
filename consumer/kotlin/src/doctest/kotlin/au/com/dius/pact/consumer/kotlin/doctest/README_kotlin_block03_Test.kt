// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 3
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.HttpInteractionDsl
import org.junit.jupiter.api.Test

class README_kotlin_block03_Test {

    private fun given(state: String) {}
    private fun uponReceiving(description: String, block: HttpInteractionDsl.() -> Unit) {
        HttpInteractionDsl().apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:3
        given("users exist")
        given("the database is healthy")
        uponReceiving("a request that needs two states") {
            withRequest { method("GET"); path("/api/data") }
            willRespondWith { status(200) }
        }
        // @DOCTEST-END
    }
}
