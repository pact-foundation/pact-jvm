// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 29
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.kotlin.doctest

import au.com.dius.pact.consumer.kotlin.pact
import au.com.dius.pact.consumer.kotlin.runConsumerTest
import au.com.dius.pact.consumer.model.MockProviderConfig
import au.com.dius.pact.core.model.PactSpecVersion
import org.junit.jupiter.api.Test

class README_kotlin_block29_Test {

    private val myPact = pact(consumer = "Block29Consumer", provider = "Block29Provider") {
        uponReceiving("a test interaction") {
            withRequest { method("GET"); path("/") }
            willRespondWith { status(200) }
        }
    }

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:29
        val result = runConsumerTest(
            pact = myPact,
            config = MockProviderConfig.createDefault(PactSpecVersion.V4)
        ) {
            // test block
        }
        // @DOCTEST-END
    }
}
