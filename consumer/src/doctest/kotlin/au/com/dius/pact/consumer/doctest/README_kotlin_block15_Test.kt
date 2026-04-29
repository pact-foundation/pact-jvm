// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 15
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest

import au.com.dius.pact.consumer.dsl.SynchronousMessagePactBuilder
import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Provider
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Doctest stub — see README.md block 15")
class README_kotlin_block15_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:15
        SynchronousMessagePactBuilder(Consumer("Some Consumer"), Provider("Some Provider"))
          .expectsToReceive("create user request")
          .reference("openapi", "operationId", "createUser")
          .toPact()
        // @DOCTEST-END
    }
}
