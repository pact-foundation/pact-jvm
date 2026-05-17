// Auto-generated — run './gradlew generateDoctests' to regenerate from README.md
// Source: README.md block 13
// Remove @Disabled once the test compiles and passes
package au.com.dius.pact.consumer.doctest

import au.com.dius.pact.consumer.dsl.PactBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Doctest stub — see README.md block 13")
class README_kotlin_block13_Test {

    @Test
    fun block() {
        // @DOCTEST-BEGIN README.md:kotlin:13
        val pact = PactBuilder("Some Consumer", "Some Provider")
          .expectsToReceiveHttpInteraction("create user") { http ->
            http
              .withRequest { req -> req.method("POST").path("/users") }
              .willRespondWith { res -> res.status(201) }
              .reference("openapi", "operationId", "createUser")
              .reference("openapi", "tag", "users")
              .reference("jira", "ticket", "PROJ-123")
          }
          .toPact()
        // @DOCTEST-END
    }
}
