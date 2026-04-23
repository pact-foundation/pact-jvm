// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 26
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.HttpInteractionDsl
import org.junit.jupiter.api.Test

class README_kotlin_block26_Test {

    private fun interaction(description: String, block: HttpInteractionDsl.() -> Unit) {
        HttpInteractionDsl().apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:26
        interaction("a privileged request") {
            given("user 42 exists")
            given("user 42 has admin role")
            withRequest { method("GET"); path("/api/admin") }
            willRespondWith { status(200) }
        }
        // @DOCTEST-END
    }
}
