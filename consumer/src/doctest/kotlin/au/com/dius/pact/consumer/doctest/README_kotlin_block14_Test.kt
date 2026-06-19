// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 14
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest

import au.com.dius.pact.consumer.dsl.PactBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
// TODO: add required imports

@Disabled("Doctest stub — see README.md block 14")
class README_kotlin_block14_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:block14
        PactBuilder("Some Consumer", "Some Provider")
          .expectsToReceiveMessageInteraction("user created event") { message ->
            message
              // .withContent(/* ... */)
              .reference("asyncapi", "messageId", "UserCreated")
          }
        // @DOCTEST-END
    }
}
