// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 21
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.newJsonObject
import org.junit.jupiter.api.Test

class README_kotlin_block21_Test {

    private fun body(body: DslPart) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:21
        body(newJsonObject {
            minArrayLike("tags", 1) {
                stringType("value", "kotlin")
            }
            maxArrayLike("aliases", 5) {
                stringType("value", "ali")
            }
        })
        // @DOCTEST-END
    }
}
