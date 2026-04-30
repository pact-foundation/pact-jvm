// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 16
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.newJsonObject
import org.junit.jupiter.api.Test

class README_kotlin_block16_Test {

    private fun body(body: DslPart) {}

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:16
        body(newJsonObject {
            stringType("name", "Alice")          // any string; example "Alice"
            numberType("age", 30)                // any number
            integerType("score", 100)            // any integer
            decimalType("balance", 9.99)         // any decimal
            booleanType("active", true)          // any boolean
            datetime("createdAt", "yyyy-MM-dd'T'HH:mm:ss")
            date("dob", "yyyy-MM-dd")
            time("startTime", "HH:mm")
            uuid("id")
            stringMatcher("postcode", "[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}", "SW1A 1AA")
            nullValue("deletedAt")
        })
        // @DOCTEST-END
    }
}
