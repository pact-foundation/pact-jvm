// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 24
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest

import au.com.dius.pact.consumer.dsl.newArray
import au.com.dius.pact.consumer.dsl.newJsonArray
import au.com.dius.pact.consumer.dsl.newObject
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 24")
class README_kotlin_block24_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:24
        newJsonArray {
            newArray {
              stringValue("a1")
              stringValue("a2")
            }
            newArray {
              numberValue(1)
              numberValue(2)
            }
            newArray {
              newObject { stringValue("foo", "Foo") }
            }
         }
        // @DOCTEST-END
    }
}
