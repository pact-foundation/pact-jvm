// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 18
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.newJsonObject
import au.com.dius.pact.consumer.dsl.newObject
import org.junit.jupiter.api.Test

class README_kotlin_block18_Test {

    private fun body(body: DslPart) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:18
        body(newJsonObject {
            stringType("name", "Alice")
            newObject("address") {
                stringType("street", "123 Main St")
                stringType("city", "Springfield")
            }
        })
        // @DOCTEST-END
    }
}
