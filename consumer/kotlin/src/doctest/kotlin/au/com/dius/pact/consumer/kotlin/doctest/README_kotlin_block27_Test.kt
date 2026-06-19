// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 27
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.HttpInteractionDsl
import org.junit.jupiter.api.Test

class README_kotlin_block27_Test {

    private fun interaction(description: String, block: HttpInteractionDsl.() -> Unit) {
        HttpInteractionDsl().apply(block)
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:27
        interaction("a future endpoint") {
            pending(true)
            withRequest {
                method("GET")
                path("/api/v2/future")
            }
            willRespondWith {
                status(200)
            }
        }
        // @DOCTEST-END
    }
}
