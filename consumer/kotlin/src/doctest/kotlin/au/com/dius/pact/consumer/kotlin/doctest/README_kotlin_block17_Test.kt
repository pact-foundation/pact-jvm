// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 17
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.newJsonObject
import org.junit.jupiter.api.Test

class README_kotlin_block17_Test {

    private fun body(body: DslPart) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:17
        body(newJsonObject {
            stringType("name", "Alice")
            `object`("address") {
                stringType("street", "123 Main St")
                stringType("city", "Springfield")
            }
        })
        // @DOCTEST-END
    }
}
