// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 24
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import org.junit.jupiter.api.Test

class README_kotlin_block24_Test {

    private fun given(state: String, vararg params: Pair<String, Any?>) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:24
        given("a user exists", "id" to "42", "role" to "admin")
        // @DOCTEST-END
    }
}
