// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 25
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import org.junit.jupiter.api.Test

class README_kotlin_block25_Test {

    private fun given(state: String, params: Map<String, Any?>) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:25
        given("a user exists", mapOf("id" to "42", "role" to "admin"))
        // @DOCTEST-END
    }
}
