// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 19
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newObject
import org.junit.jupiter.api.Test

class README_kotlin_block19_Test {

    private fun body(body: DslPart) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:19
        body(newJsonArray {
            newObject {
                stringType("id", "abc-123")
                stringType("name", "Widget")
            }
        })
        // @DOCTEST-END
    }
}
